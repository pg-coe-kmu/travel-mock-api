package com.coe.b04.server.service;

import com.coe.b04.server.enums.ReservationStatus;
import com.coe.b04.server.enums.ServiceType;
import com.coe.b04.server.io.CreateReservationRequest;
import com.coe.b04.server.io.ReservationDetailsResponse;
import com.coe.b04.server.io.ReservationResponse;
import com.coe.b04.server.model.Car;
import com.coe.b04.server.model.CarProvider;
import com.coe.b04.server.model.Flight;
import com.coe.b04.server.model.Hotel;
import com.coe.b04.server.model.MaxOccupancy;
import com.coe.b04.server.model.Reservation;
import com.coe.b04.server.model.ReservationItem;
import com.coe.b04.server.model.RoomType;
import com.coe.b04.server.repository.CarRepository;
import com.coe.b04.server.repository.FlightRepository;
import com.coe.b04.server.repository.HotelRepository;
import com.coe.b04.server.repository.ReservationRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Service
public class ReservationService {

    private static final int VALIDITY_MINUTES = 30;
    // Alphabet ohne I, O, 0, 1 (Verwechslungsgefahr beim Ablesen)
    private static final String NUMBER_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int NUMBER_RANDOM_LENGTH = 6;
    private static final DateTimeFormatter NUMBER_DATE_FORMAT = DateTimeFormatter.BASIC_ISO_DATE;

    private final ReservationRepository reservationRepository;
    private final CarRepository carRepository;
    private final HotelRepository hotelRepository;
    private final FlightRepository flightRepository;
    private final SecureRandom random = new SecureRandom();

    public ReservationService(ReservationRepository reservationRepository,
                              CarRepository carRepository,
                              HotelRepository hotelRepository,
                              FlightRepository flightRepository) {
        this.reservationRepository = reservationRepository;
        this.carRepository = carRepository;
        this.hotelRepository = hotelRepository;
        this.flightRepository = flightRepository;
    }

    /**
     * Erstellt eine 30-Minuten-Reservation fuer das gewaehlte Angebot.
     * Preise werden aus den aktuellen Mock-Daten geladen, berechnet und
     * eingefroren (total_price = Summe der Leistungen, App-Validierung).
     */
    public ReservationResponse create(CreateReservationRequest request) {
        validateCombination(request);

        List<ReservationItem> items = new ArrayList<>();
        BigDecimal totalPrice = BigDecimal.ZERO;

        if (request.getFlightId() != null) {
            Flight flight = requireFlight(request.getFlightId(), request);
            items.add(flightItem(ServiceType.FLIGHT, flight));
            totalPrice = totalPrice.add(flight.getPrice());
        }
        if (request.getReturnFlightId() != null) {
            Flight returnFlight = requireFlight(request.getReturnFlightId(), request);
            items.add(flightItem(ServiceType.RETURN_FLIGHT, returnFlight));
            totalPrice = totalPrice.add(returnFlight.getPrice());
        }
        if (request.getHotelId() != null) {
            ReservationItem hotelItem = hotelItem(requireHotel(request), request);
            items.add(hotelItem);
            totalPrice = totalPrice.add(hotelItem.getPrice());
        }
        if (request.getCarId() != null) {
            ReservationItem carItem = carItem(requireCar(request), request);
            items.add(carItem);
            totalPrice = totalPrice.add(carItem.getPrice());
        }

        OffsetDateTime createdAt = OffsetDateTime.now();
        Reservation reservation = Reservation.builder()
                .reservationNumber(generateReservationNumber())
                .status(ReservationStatus.PENDING)
                .origin(request.getOrigin())
                .destination(request.getDestination())
                .departureDate(request.getDepartureDate())
                .returnDate(request.getReturnDate())
                .adults(request.getAdults())
                .children(request.getChildren() == null ? 0 : request.getChildren())
                .infants(request.getInfants() == null ? 0 : request.getInfants())
                .currency(request.getCurrency().toUpperCase())
                .totalPrice(totalPrice)
                .createdAt(createdAt)
                .expiresAt(createdAt.plusMinutes(VALIDITY_MINUTES))
                .services(items)
                .build();

        return toResponse(reservationRepository.save(reservation));
    }

    public ReservationResponse getByNumber(String reservationNumber) {
        Reservation reservation = findOrThrow(reservationNumber);
        applyLazyExpiry(reservation);
        return toResponse(reservation);
    }

    /**
     * Wie getByNumber, zusaetzlich die vollen Angebotsinhalte aus den
     * Mock-APIs. Sind die referenzierten IDs dort nicht mehr vorhanden
     * (Mock-Daten geaendert), bleibt der jeweilige Block null.
     */
    public ReservationDetailsResponse getDetails(String reservationNumber) {
        Reservation reservation = findOrThrow(reservationNumber);
        applyLazyExpiry(reservation);
        ReservationResponse base = toResponse(reservation);

        Flight outbound = null;
        Flight returnFlight = null;
        Hotel hotel = null;
        CarProvider car = null;
        for (ReservationItem item : reservation.getServices()) {
            switch (item.getServiceType()) {
                case FLIGHT -> outbound = flightRepository.findById(item.getServiceId());
                case RETURN_FLIGHT -> returnFlight = flightRepository.findById(item.getServiceId());
                case HOTEL -> hotel = hotelRepository.findByHotelIdAndRoomId(item.getServiceId(), item.getRoomId());
                case CAR -> car = carRepository.findByProviderIdAndCarId(item.getProviderId(), item.getServiceId());
            }
        }

        ReservationDetailsResponse.FlightDetails flight = (outbound != null || returnFlight != null)
                ? new ReservationDetailsResponse.FlightDetails(outbound, returnFlight)
                : null;

        return ReservationDetailsResponse.from(base, flight, hotel, car);
    }

    public ReservationResponse cancel(String reservationNumber) {
        Reservation reservation = findOrThrow(reservationNumber);
        applyLazyExpiry(reservation);
        if (reservation.getStatus() != ReservationStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Reservation cannot be cancelled, status: " + reservation.getStatus());
        }
        // ponytail: check-then-act Race zwischen Cancel und Spaeter-Payment;
        // atomarer "UPDATE ... WHERE status = 'PENDING'" sobald Payment existiert
        reservation.setStatus(ReservationStatus.CANCELLED);
        reservation.setCancelledAt(OffsetDateTime.now());
        reservationRepository.updateStatus(reservation.getId(), ReservationStatus.CANCELLED, reservation.getCancelledAt());
        return toResponse(reservation);
    }

    // ---------- Validierung ----------

    private void validateCombination(CreateReservationRequest request) {
        boolean hasFlight = request.getFlightId() != null;
        boolean hasHotel = request.getHotelId() != null;
        boolean hasCar = request.getCarId() != null;
        if (!hasFlight && !hasHotel && !hasCar) {
            throw badRequest("At least one of flightId, hotelId or carId is required");
        }
        if (request.getReturnFlightId() != null && !hasFlight) {
            throw badRequest("returnFlightId requires flightId");
        }
        if (request.getReturnFlightId() != null && request.getReturnDate() == null) {
            throw badRequest("returnFlightId requires returnDate");
        }
        if (hasHotel != (request.getRoomId() != null)) {
            throw badRequest("roomId and hotelId must be provided together");
        }
        if (hasCar != (request.getProviderId() != null)) {
            throw badRequest("providerId and carId must be provided together");
        }
        if (request.getReturnDate() != null && request.getReturnDate().isBefore(request.getDepartureDate())) {
            throw badRequest("returnDate must not be before departureDate");
        }
    }

    private Flight requireFlight(String flightId, CreateReservationRequest request) {
        Flight flight = flightRepository.findById(flightId);
        if (flight == null) {
            throw badRequest("Unknown flightId: " + flightId);
        }
        requireCurrency(request, flight.getCurrency(), "flight " + flightId);
        return flight;
    }

    private Hotel requireHotel(CreateReservationRequest request) {
        Hotel hotel = hotelRepository.findByHotelIdAndRoomId(request.getHotelId(), request.getRoomId());
        if (hotel == null) {
            throw badRequest("Unknown hotelId/roomId: " + request.getHotelId() + " / " + request.getRoomId());
        }
        RoomType room = hotel.getRoomTypes().getFirst();
        MaxOccupancy occupancy = room.getMaxOccupancy();
        int children = request.getChildren() == null ? 0 : request.getChildren();
        if (occupancy == null || request.getAdults() > occupancy.getAdults() || children > occupancy.getChildren()) {
            throw badRequest("Persons do not fit the max occupancy of room " + room.getRoomId());
        }
        requireCurrency(request, hotel.getBaseCurrency(), "hotel " + request.getHotelId());
        return hotel;
    }

    private CarProvider requireCar(CreateReservationRequest request) {
        CarProvider provider = carRepository.findByProviderIdAndCarId(request.getProviderId(), request.getCarId());
        if (provider == null) {
            throw badRequest("Unknown providerId/carId: " + request.getProviderId() + " / " + request.getCarId());
        }
        requireCurrency(request, provider.getBaseCurrency(), "car " + request.getCarId());
        return provider;
    }

    private void requireCurrency(CreateReservationRequest request, String actual, String what) {
        if (actual == null || !actual.equalsIgnoreCase(request.getCurrency())) {
            throw badRequest("Currency mismatch for " + what + ": expected " + request.getCurrency() + ", found " + actual);
        }
    }

    private ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    // ---------- Items ----------

    private ReservationItem flightItem(ServiceType type, Flight flight) {
        return ReservationItem.builder()
                .serviceType(type)
                .serviceId(flight.getFlightId())
                .price(flight.getPrice())
                .build();
    }

    private ReservationItem hotelItem(Hotel hotel, CreateReservationRequest request) {
        RoomType room = hotel.getRoomTypes().getFirst();
        LocalDate checkIn = request.getDepartureDate();
        LocalDate checkOut = effectiveReturnDate(request);
        long nights = ChronoUnit.DAYS.between(checkIn, checkOut);
        return ReservationItem.builder()
                .serviceType(ServiceType.HOTEL)
                .serviceId(hotel.getHotelId())
                .roomId(room.getRoomId())
                .checkIn(checkIn)
                .checkOut(checkOut)
                .price(room.getPricePerNight().multiply(BigDecimal.valueOf(nights)))
                .build();
    }

    private ReservationItem carItem(CarProvider provider, CreateReservationRequest request) {
        Car car = provider.getCars().getFirst();
        LocalDate pickupDate = request.getDepartureDate();
        LocalDate returnDate = effectiveReturnDate(request);
        long days = ChronoUnit.DAYS.between(pickupDate, returnDate);
        return ReservationItem.builder()
                .serviceType(ServiceType.CAR)
                .serviceId(car.getCarId())
                .providerId(provider.getProviderId())
                .pickupDate(pickupDate)
                .returnDate(returnDate)
                .pickupLocation(car.getLocations().getPickupLocation().getName())
                .returnLocation(car.getLocations().getReturnLocation().getName())
                .price(car.getPricing().getPricePerDay().multiply(BigDecimal.valueOf(days)))
                .build();
    }

    private LocalDate effectiveReturnDate(CreateReservationRequest request) {
        return request.getReturnDate() != null ? request.getReturnDate() : request.getDepartureDate().plusDays(1);
    }

    // ---------- Reservation-Number / Expiry ----------

    private String generateReservationNumber() {
        for (int attempt = 0; attempt < 10; attempt++) {
            StringBuilder suffix = new StringBuilder(NUMBER_RANDOM_LENGTH);
            for (int i = 0; i < NUMBER_RANDOM_LENGTH; i++) {
                suffix.append(NUMBER_ALPHABET.charAt(random.nextInt(NUMBER_ALPHABET.length())));
            }
            String number = "RES-" + NUMBER_DATE_FORMAT.format(LocalDate.now()) + "-" + suffix;
            if (reservationRepository.findByReservationNumber(number).isEmpty()) {
                return number;
            }
        }
        throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                "Could not generate a unique reservation number");
    }

    private Reservation findOrThrow(String reservationNumber) {
        return reservationRepository.findByReservationNumber(reservationNumber)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No reservation found for reservationNumber: " + reservationNumber));
    }

    private void applyLazyExpiry(Reservation reservation) {
        if (reservation.getStatus() == ReservationStatus.PENDING
                && !reservation.getExpiresAt().isAfter(OffsetDateTime.now())) {
            reservation.setStatus(ReservationStatus.EXPIRED);
            reservationRepository.updateStatus(reservation.getId(), ReservationStatus.EXPIRED, null);
        }
    }

    // ---------- Mapping ----------

    private ReservationResponse toResponse(Reservation reservation) {
        return ReservationResponse.builder()
                .reservationNumber(reservation.getReservationNumber())
                .status(reservation.getStatus())
                .createdAt(reservation.getCreatedAt())
                .expiresAt(reservation.getExpiresAt())
                .expiresInSeconds(Math.max(0,
                        Duration.between(OffsetDateTime.now(), reservation.getExpiresAt()).getSeconds()))
                .trip(ReservationResponse.Trip.builder()
                        .origin(reservation.getOrigin())
                        .destination(reservation.getDestination())
                        .departureDate(reservation.getDepartureDate())
                        .returnDate(reservation.getReturnDate())
                        .adults(reservation.getAdults())
                        .children(reservation.getChildren())
                        .infants(reservation.getInfants())
                        .build())
                .services(reservation.getServices().stream()
                        .map(item -> ReservationResponse.ServiceItemResponse.builder()
                                .serviceType(item.getServiceType())
                                .serviceId(item.getServiceId())
                                .providerId(item.getProviderId())
                                .price(item.getPrice())
                                .roomId(item.getRoomId())
                                .checkIn(item.getCheckIn())
                                .checkOut(item.getCheckOut())
                                .pickupDate(item.getPickupDate())
                                .returnDate(item.getReturnDate())
                                .pickupLocation(item.getPickupLocation())
                                .returnLocation(item.getReturnLocation())
                                .build())
                        .toList())
                .price(ReservationResponse.Price.builder()
                        .totalPrice(reservation.getTotalPrice())
                        .currency(reservation.getCurrency())
                        .flightPrice(sumPrices(reservation, ServiceType.FLIGHT, ServiceType.RETURN_FLIGHT))
                        .hotelPrice(sumPrices(reservation, ServiceType.HOTEL))
                        .carPrice(sumPrices(reservation, ServiceType.CAR))
                        .build())
                .build();
    }

    private BigDecimal sumPrices(Reservation reservation, ServiceType... types) {
        List<ServiceType> typeList = Arrays.asList(types);
        boolean present = reservation.getServices().stream().anyMatch(item -> typeList.contains(item.getServiceType()));
        if (!present) {
            return null;
        }
        return reservation.getServices().stream()
                .filter(item -> typeList.contains(item.getServiceType()))
                .map(ReservationItem::getPrice)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
