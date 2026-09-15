package com.coe.b04.server.service;

import com.coe.b04.server.enums.Direction;
import com.coe.b04.server.enums.ReservationStatus;
import com.coe.b04.server.enums.ServiceType;
import com.coe.b04.server.io.CreateReservationRequest;
import com.coe.b04.server.io.ReservationResponse;
import com.coe.b04.server.model.Car;
import com.coe.b04.server.model.CarProvider;
import com.coe.b04.server.model.Flight;
import com.coe.b04.server.model.Hotel;
import com.coe.b04.server.model.MaxOccupancy;
import com.coe.b04.server.model.Reservation;
import com.coe.b04.server.model.ReservationCarDetail;
import com.coe.b04.server.model.ReservationFlightDetail;
import com.coe.b04.server.model.ReservationHotelDetail;
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
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

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
     * eingefroren (total_price = Summe der Items, App-Validierung).
     * Aus dem Request entstehen generische Items + Detaildatensaetze.
     */
    public ReservationResponse create(CreateReservationRequest request) {
        validateCombination(request);

        List<ReservationItem> items = new ArrayList<>();
        BigDecimal totalPrice = BigDecimal.ZERO;

        if (request.getFlightId() != null) {
            ReservationItem item = flightItem(Direction.OUTBOUND, requireFlight(request.getFlightId(), request));
            items.add(item);
            totalPrice = totalPrice.add(item.getPrice());
        }
        if (request.getReturnFlightId() != null) {
            ReservationItem item = flightItem(Direction.RETURN, requireFlight(request.getReturnFlightId(), request));
            items.add(item);
            totalPrice = totalPrice.add(item.getPrice());
        }
        if (request.getHotelId() != null) {
            ReservationItem item = hotelItem(requireHotel(request), request);
            items.add(item);
            totalPrice = totalPrice.add(item.getPrice());
        }
        if (request.getCarId() != null) {
            ReservationItem item = carItem(requireCar(request), request);
            items.add(item);
            totalPrice = totalPrice.add(item.getPrice());
        }

        OffsetDateTime createdAt = OffsetDateTime.now();
        Reservation reservation = Reservation.builder()
                .reservationNumber(generateReservationNumber())
                .status(ReservationStatus.PENDING)
                .origin(request.getOrigin())
                .destination(request.getDestination())
                .adults(request.getAdults())
                .children(request.getChildren() == null ? 0 : request.getChildren())
                .infants(request.getInfants() == null ? 0 : request.getInfants())
                .currency(request.getCurrency().toUpperCase())
                .totalPrice(totalPrice)
                .createdAt(createdAt)
                .expiresAt(createdAt.plusMinutes(VALIDITY_MINUTES))
                .items(items)
                .build();

        return toResponse(reservationRepository.save(reservation));
    }

    public ReservationResponse getByNumber(String reservationNumber) {
        Reservation reservation = applyLazyExpiry(findOrThrow(reservationNumber));
        return toResponse(reservation);
    }

    public ReservationResponse cancel(String reservationNumber) {
        Reservation reservation = applyLazyExpiry(findOrThrow(reservationNumber));
        if (reservation.getStatus() != ReservationStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Reservation cannot be cancelled, status: " + reservation.getStatus());
        }
        // Atomarer Cancel in der DB (nur wenn noch PENDING und laut DB-Uhr
        // nicht abgelaufen) - verliert gegen concurrente Aenderungen und liefert 409
        OffsetDateTime cancelledAt = OffsetDateTime.now();
        if (!reservationRepository.cancel(reservation.getId(), cancelledAt)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Reservation was modified concurrently, status is no longer PENDING");
        }
        reservation.setStatus(ReservationStatus.CANCELLED);
        reservation.setCancelledAt(cancelledAt);
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

    private ReservationItem flightItem(Direction direction, Flight flight) {
        return ReservationItem.builder()
                .itemType(ServiceType.FLIGHT)
                .price(flight.getPrice())
                .flight(ReservationFlightDetail.builder()
                        .direction(direction)
                        .flightId(flight.getFlightId())
                        .airline(flight.getAirline())
                        .flightNumber(flight.getFlightNumber())
                        .departureAirport(flight.getDepartureAirport())
                        .arrivalAirport(flight.getArrivalAirport())
                        .departureAt(toUtc(flight.getDepartureTime()))
                        .arrivalAt(toUtc(flight.getArrivalTime()))
                        .build())
                .build();
    }

    private ReservationItem hotelItem(Hotel hotel, CreateReservationRequest request) {
        RoomType room = hotel.getRoomTypes().getFirst();
        LocalDate checkIn = request.getDepartureDate();
        LocalDate checkOut = effectiveReturnDate(request);
        long nights = ChronoUnit.DAYS.between(checkIn, checkOut);
        return ReservationItem.builder()
                .itemType(ServiceType.HOTEL)
                .price(room.getPricePerNight().multiply(BigDecimal.valueOf(nights)))
                .hotel(ReservationHotelDetail.builder()
                        .hotelId(hotel.getHotelId())
                        .roomId(room.getRoomId())
                        .checkIn(checkIn)
                        .checkOut(checkOut)
                        .hotelName(hotel.getName())
                        .roomName(room.getRoomType())
                        .build())
                .build();
    }

    private ReservationItem carItem(CarProvider provider, CreateReservationRequest request) {
        Car car = provider.getCars().getFirst();
        LocalDate pickupDate = request.getDepartureDate();
        LocalDate returnDate = effectiveReturnDate(request);
        long days = ChronoUnit.DAYS.between(pickupDate, returnDate);
        return ReservationItem.builder()
                .itemType(ServiceType.CAR)
                .price(car.getPricing().getPricePerDay().multiply(BigDecimal.valueOf(days)))
                .car(ReservationCarDetail.builder()
                        .carId(car.getCarId())
                        .providerId(provider.getProviderId())
                        .pickupAt(toUtc(pickupDate.atStartOfDay()))
                        .returnAt(toUtc(returnDate.atStartOfDay()))
                        .pickupLocation(car.getLocations().getPickupLocation().getName())
                        .returnLocation(car.getLocations().getReturnLocation().getName())
                        .vehicleName(car.getBrand() + " " + car.getModel())
                        .build())
                .build();
    }

    private OffsetDateTime toUtc(LocalDateTime dateTime) {
        return dateTime == null ? null : dateTime.atOffset(ZoneOffset.UTC);
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

    /**
     * Setzt abgelaufene PENDING-Reservations lazy auf EXPIRED.
     * Verliert der bedingte UPDATE gegen einen concurrenten Cancel,
     * wird der in der DB gewonnene Status neu geladen.
     */
    private Reservation applyLazyExpiry(Reservation reservation) {
        if (reservation.getStatus() == ReservationStatus.PENDING
                && !reservation.getExpiresAt().isAfter(OffsetDateTime.now())) {
            if (reservationRepository.updateStatus(reservation.getId(), ReservationStatus.EXPIRED)) {
                reservation.setStatus(ReservationStatus.EXPIRED);
            } else {
                return findOrThrow(reservation.getReservationNumber());
            }
        }
        return reservation;
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
                        .adults(reservation.getAdults())
                        .children(reservation.getChildren())
                        .infants(reservation.getInfants())
                        .build())
                .items(reservation.getItems().stream()
                        .map(item -> ReservationResponse.ItemResponse.builder()
                                .type(item.getItemType())
                                .price(item.getPrice())
                                .flight(item.getFlight())
                                .hotel(item.getHotel())
                                .car(item.getCar())
                                .build())
                        .toList())
                .price(ReservationResponse.Price.builder()
                        .totalPrice(reservation.getTotalPrice())
                        .currency(reservation.getCurrency())
                        .build())
                .build();
    }
}
