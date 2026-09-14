package com.coe.b04.server.service;

import com.coe.b04.server.enums.ReservationStatus;
import com.coe.b04.server.enums.ServiceType;
import com.coe.b04.server.io.CreateReservationRequest;
import com.coe.b04.server.io.ReservationDetailsResponse;
import com.coe.b04.server.io.ReservationResponse;
import com.coe.b04.server.model.Car;
import com.coe.b04.server.model.CarLocation;
import com.coe.b04.server.model.CarLocations;
import com.coe.b04.server.model.CarPricing;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ReservationServiceTest {

    private ReservationService reservationService;
    private ReservationRepository reservationRepository;
    private CarRepository carRepository;
    private HotelRepository hotelRepository;
    private FlightRepository flightRepository;

    @BeforeEach
    void setUp() {
        reservationRepository = mock(ReservationRepository.class);
        carRepository = mock(CarRepository.class);
        hotelRepository = mock(HotelRepository.class);
        flightRepository = mock(FlightRepository.class);
        reservationService = new ReservationService(reservationRepository, carRepository, hotelRepository, flightRepository);
        // save() gibt die uebergebene Reservation zurueck (kein DB-Roundtrip im Test)
        when(reservationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    // ---------- Fixtures ----------

    private CreateReservationRequest fullRequest() {
        return CreateReservationRequest.builder()
                .origin("Frankfurt am Main")
                .destination("Madrid")
                .departureDate(LocalDate.of(2026, 10, 10))
                .returnDate(LocalDate.of(2026, 10, 17))
                .adults(2)
                .children(0)
                .infants(0)
                .flightId("FL-1")
                .returnFlightId("FL-2")
                .hotelId("HOT-1")
                .roomId("ROOM-1")
                .carId("CAR-1")
                .providerId("PROV-1")
                .currency("EUR")
                .build();
    }

    private Flight flight(String id, String price, String currency) {
        Flight flight = new Flight();
        flight.setFlightId(id);
        flight.setPrice(new BigDecimal(price));
        flight.setCurrency(currency);
        return flight;
    }

    private Hotel hotelWithRoom(String hotelId, String roomId, String pricePerNight,
                                String currency, int maxAdults, int maxChildren) {
        RoomType room = new RoomType();
        room.setRoomId(roomId);
        room.setPricePerNight(new BigDecimal(pricePerNight));
        room.setMaxOccupancy(new MaxOccupancy(maxAdults, maxChildren));
        return Hotel.builder()
                .hotelId(hotelId)
                .baseCurrency(currency)
                .roomTypes(List.of(room))
                .build();
    }

    private CarProvider providerWithCar(String providerId, String carId, String pricePerDay, String currency) {
        CarLocation location = new CarLocation("LOC-1", "Madrid Airport", "Madrid", "Address", "07:00 - 23:00");
        Car car = Car.builder()
                .carId(carId)
                .pricing(new CarPricing(new BigDecimal(pricePerDay), new BigDecimal(pricePerDay), 1))
                .locations(new CarLocations(location, location))
                .build();
        return CarProvider.builder()
                .providerId(providerId)
                .baseCurrency(currency)
                .cars(List.of(car))
                .build();
    }

    private void stubFullOffer() {
        when(flightRepository.findById("FL-1")).thenReturn(flight("FL-1", "289.00", "EUR"));
        when(flightRepository.findById("FL-2")).thenReturn(flight("FL-2", "311.00", "EUR"));
        when(hotelRepository.findByHotelIdAndRoomId("HOT-1", "ROOM-1"))
                .thenReturn(hotelWithRoom("HOT-1", "ROOM-1", "44.00", "EUR", 2, 1));
        when(carRepository.findByProviderIdAndCarId("PROV-1", "CAR-1"))
                .thenReturn(providerWithCar("PROV-1", "CAR-1", "28.50", "EUR"));
    }

    private Reservation reservation(String number, ReservationStatus status, OffsetDateTime expiresAt) {
        return Reservation.builder()
                .id(UUID.randomUUID())
                .reservationNumber(number)
                .status(status)
                .origin("Frankfurt am Main")
                .destination("Madrid")
                .departureDate(LocalDate.of(2026, 10, 10))
                .returnDate(LocalDate.of(2026, 10, 17))
                .adults(2)
                .currency("EUR")
                .totalPrice(new BigDecimal("1107.50"))
                .createdAt(OffsetDateTime.now().minusMinutes(30))
                .expiresAt(expiresAt)
                .services(List.of(
                        ReservationItem.builder().serviceType(ServiceType.FLIGHT).serviceId("FL-1").price(new BigDecimal("289.00")).build(),
                        ReservationItem.builder().serviceType(ServiceType.RETURN_FLIGHT).serviceId("FL-2").price(new BigDecimal("311.00")).build(),
                        ReservationItem.builder().serviceType(ServiceType.HOTEL).serviceId("HOT-1").roomId("ROOM-1")
                                .checkIn(LocalDate.of(2026, 10, 10)).checkOut(LocalDate.of(2026, 10, 17))
                                .price(new BigDecimal("308.00")).build(),
                        ReservationItem.builder().serviceType(ServiceType.CAR).serviceId("CAR-1").providerId("PROV-1")
                                .pickupDate(LocalDate.of(2026, 10, 10)).returnDate(LocalDate.of(2026, 10, 17))
                                .pickupLocation("Madrid Airport").returnLocation("Madrid Airport")
                                .price(new BigDecimal("199.50")).build()))
                .build();
    }

    private ResponseStatusException assertBadRequest(Runnable action) {
        ResponseStatusException e = assertThrows(ResponseStatusException.class, action::run);
        assertEquals(HttpStatus.BAD_REQUEST, e.getStatusCode());
        return e;
    }

    // ---------- create ----------

    @Test
    void createComputesAndFreezesPrices() {
        stubFullOffer();

        ReservationResponse response = reservationService.create(fullRequest());

        // 289 + 311 + 7 * 44 + 7 * 28.50 = 1107.50
        assertEquals(0, new BigDecimal("1107.50").compareTo(response.getPrice().getTotalPrice()));
        assertEquals(0, new BigDecimal("600.00").compareTo(response.getPrice().getFlightPrice()));
        assertEquals(0, new BigDecimal("308.00").compareTo(response.getPrice().getHotelPrice()));
        assertEquals(0, new BigDecimal("199.50").compareTo(response.getPrice().getCarPrice()));
        assertEquals("EUR", response.getPrice().getCurrency());
        assertEquals(ReservationStatus.PENDING, response.getStatus());
        assertEquals(4, response.getServices().size());
        assertTrue(response.getReservationNumber().matches("RES-\\d{8}-[A-HJ-NP-Z2-9]{6}"));
        assertTrue(response.getExpiresAt().isAfter(response.getCreatedAt()));
        verify(reservationRepository).save(any());
    }

    @Test
    void createHotelOnlyDerivesCheckInCheckOut() {
        when(hotelRepository.findByHotelIdAndRoomId("HOT-1", "ROOM-1"))
                .thenReturn(hotelWithRoom("HOT-1", "ROOM-1", "50.00", "EUR", 2, 1));

        CreateReservationRequest request = CreateReservationRequest.builder()
                .origin("Frankfurt am Main")
                .destination("Madrid")
                .departureDate(LocalDate.of(2026, 10, 10))
                .adults(1)
                .currency("EUR")
                .hotelId("HOT-1")
                .roomId("ROOM-1")
                .build();

        ReservationResponse response = reservationService.create(request);

        assertEquals(1, response.getServices().size());
        ReservationResponse.ServiceItemResponse hotel = response.getServices().getFirst();
        assertEquals(ServiceType.HOTEL, hotel.getServiceType());
        assertEquals("ROOM-1", hotel.getRoomId());
        assertEquals(LocalDate.of(2026, 10, 10), hotel.getCheckIn());
        // ohne Rueckreise: eine Nacht
        assertEquals(LocalDate.of(2026, 10, 11), hotel.getCheckOut());
        assertEquals(0, new BigDecimal("50.00").compareTo(response.getPrice().getHotelPrice()));
        assertNull(response.getPrice().getFlightPrice());
        assertNull(response.getPrice().getCarPrice());
    }

    @Test
    void createFlightOnly() {
        when(flightRepository.findById("FL-1")).thenReturn(flight("FL-1", "100.00", "EUR"));
        when(flightRepository.findById("FL-2")).thenReturn(flight("FL-2", "80.00", "EUR"));

        CreateReservationRequest request = CreateReservationRequest.builder()
                .origin("Frankfurt am Main")
                .destination("Madrid")
                .departureDate(LocalDate.of(2026, 10, 10))
                .returnDate(LocalDate.of(2026, 10, 17))
                .adults(2)
                .currency("EUR")
                .flightId("FL-1")
                .returnFlightId("FL-2")
                .build();

        ReservationResponse response = reservationService.create(request);

        assertEquals(2, response.getServices().size());
        assertEquals(0, new BigDecimal("180.00").compareTo(response.getPrice().getFlightPrice()));
        assertNull(response.getPrice().getHotelPrice());
        assertNull(response.getPrice().getCarPrice());
    }

    @Test
    void createRejectsRequestWithoutAnyService() {
        CreateReservationRequest request = CreateReservationRequest.builder()
                .origin("Frankfurt am Main")
                .destination("Madrid")
                .departureDate(LocalDate.of(2026, 10, 10))
                .adults(2)
                .currency("EUR")
                .build();

        assertBadRequest(() -> reservationService.create(request));
        verifyNoInteractions(reservationRepository);
    }

    @Test
    void createRejectsReturnFlightWithoutFlight() {
        CreateReservationRequest request = CreateReservationRequest.builder()
                .origin("Frankfurt am Main").destination("Madrid")
                .departureDate(LocalDate.of(2026, 10, 10)).returnDate(LocalDate.of(2026, 10, 17))
                .adults(2).currency("EUR")
                .returnFlightId("FL-2")
                .build();

        assertBadRequest(() -> reservationService.create(request));
    }

    @Test
    void createRejectsReturnFlightWithoutReturnDate() {
        CreateReservationRequest request = CreateReservationRequest.builder()
                .origin("Frankfurt am Main").destination("Madrid")
                .departureDate(LocalDate.of(2026, 10, 10))
                .adults(2).currency("EUR")
                .flightId("FL-1").returnFlightId("FL-2")
                .build();

        assertBadRequest(() -> reservationService.create(request));
    }

    @Test
    void createRejectsRoomIdWithoutHotelAndViceVersa() {
        assertBadRequest(() -> reservationService.create(
                CreateReservationRequest.builder()
                        .origin("Frankfurt am Main").destination("Madrid")
                        .departureDate(LocalDate.of(2026, 10, 10))
                        .adults(2).currency("EUR")
                        .roomId("ROOM-1")
                        .build()));

        assertBadRequest(() -> reservationService.create(
                CreateReservationRequest.builder()
                        .origin("Frankfurt am Main").destination("Madrid")
                        .departureDate(LocalDate.of(2026, 10, 10))
                        .adults(2).currency("EUR")
                        .hotelId("HOT-1")
                        .build()));
    }

    @Test
    void createRejectsCarWithoutProviderAndViceVersa() {
        assertBadRequest(() -> reservationService.create(
                CreateReservationRequest.builder()
                        .origin("Frankfurt am Main").destination("Madrid")
                        .departureDate(LocalDate.of(2026, 10, 10))
                        .adults(2).currency("EUR")
                        .carId("CAR-1")
                        .build()));

        assertBadRequest(() -> reservationService.create(
                CreateReservationRequest.builder()
                        .origin("Frankfurt am Main").destination("Madrid")
                        .departureDate(LocalDate.of(2026, 10, 10))
                        .adults(2).currency("EUR")
                        .providerId("PROV-1")
                        .build()));
    }

    @Test
    void createRejectsUnknownIds() {
        stubFullOffer();
        CreateReservationRequest request = fullRequest().toBuilder().flightId("FL-UNKNOWN").build();

        assertBadRequest(() -> reservationService.create(request));
        verify(reservationRepository, never()).save(any());
    }

    @Test
    void createRejectsOccupancyExceeded() {
        when(hotelRepository.findByHotelIdAndRoomId("HOT-1", "ROOM-1"))
                .thenReturn(hotelWithRoom("HOT-1", "ROOM-1", "50.00", "EUR", 1, 0));

        CreateReservationRequest request = CreateReservationRequest.builder()
                .origin("Frankfurt am Main").destination("Madrid")
                .departureDate(LocalDate.of(2026, 10, 10))
                .adults(2).currency("EUR")
                .hotelId("HOT-1").roomId("ROOM-1")
                .build();

        assertBadRequest(() -> reservationService.create(request));
    }

    @Test
    void createRejectsCurrencyMismatch() {
        when(flightRepository.findById("FL-1")).thenReturn(flight("FL-1", "100.00", "USD"));

        CreateReservationRequest request = CreateReservationRequest.builder()
                .origin("Frankfurt am Main").destination("Madrid")
                .departureDate(LocalDate.of(2026, 10, 10))
                .adults(1).currency("EUR")
                .flightId("FL-1")
                .build();

        assertBadRequest(() -> reservationService.create(request));
    }

    @Test
    void createRetriesOnReservationNumberCollision() {
        stubFullOffer();
        when(reservationRepository.findByReservationNumber(anyString()))
                .thenReturn(Optional.of(reservation("RES-COLLISION", ReservationStatus.PENDING,
                        OffsetDateTime.now().plusMinutes(30))))
                .thenReturn(Optional.empty());

        ReservationResponse response = reservationService.create(fullRequest());

        assertNotEquals("RES-COLLISION", response.getReservationNumber());
    }

    // ---------- get ----------

    @Test
    void getByNumberReturnsStoredReservation() {
        when(reservationRepository.findByReservationNumber("RES-X"))
                .thenReturn(Optional.of(reservation("RES-X", ReservationStatus.PENDING,
                        OffsetDateTime.now().plusMinutes(30))));

        ReservationResponse response = reservationService.getByNumber("RES-X");

        assertEquals("RES-X", response.getReservationNumber());
        assertEquals(ReservationStatus.PENDING, response.getStatus());
        assertEquals(4, response.getServices().size());
    }

    @Test
    void getByNumberUnknownThrows404() {
        when(reservationRepository.findByReservationNumber("RES-X")).thenReturn(Optional.empty());

        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> reservationService.getByNumber("RES-X"));

        assertEquals(HttpStatus.NOT_FOUND, e.getStatusCode());
    }

    @Test
    void getByNumberMarksExpiredLazily() {
        Reservation reservation = reservation("RES-X", ReservationStatus.PENDING,
                OffsetDateTime.now().minusMinutes(1));
        when(reservationRepository.findByReservationNumber("RES-X")).thenReturn(Optional.of(reservation));

        ReservationResponse response = reservationService.getByNumber("RES-X");

        assertEquals(ReservationStatus.EXPIRED, response.getStatus());
        verify(reservationRepository).updateStatus(reservation.getId(), ReservationStatus.EXPIRED, null);
    }

    @Test
    void getDetailsResolvesOfferContents() {
        stubFullOffer();
        when(reservationRepository.findByReservationNumber("RES-X"))
                .thenReturn(Optional.of(reservation("RES-X", ReservationStatus.PENDING,
                        OffsetDateTime.now().plusMinutes(30))));

        ReservationDetailsResponse response = reservationService.getDetails("RES-X");

        assertEquals("FL-1", response.getFlight().getOutbound().getFlightId());
        assertEquals("FL-2", response.getFlight().getReturnFlight().getFlightId());
        assertEquals("HOT-1", response.getHotel().getHotelId());
        assertEquals("CAR-1", response.getCar().getCars().getFirst().getCarId());
        assertEquals(0, new BigDecimal("1107.50").compareTo(response.getPrice().getTotalPrice()));
    }

    // ---------- cancel ----------

    @Test
    void cancelTransitionsPendingToCancelled() {
        Reservation reservation = reservation("RES-X", ReservationStatus.PENDING,
                OffsetDateTime.now().plusMinutes(30));
        when(reservationRepository.findByReservationNumber("RES-X")).thenReturn(Optional.of(reservation));

        ReservationResponse response = reservationService.cancel("RES-X");

        assertEquals(ReservationStatus.CANCELLED, response.getStatus());
        verify(reservationRepository)
                .updateStatus(eq(reservation.getId()), eq(ReservationStatus.CANCELLED), any(OffsetDateTime.class));
    }

    @Test
    void cancelExpiredThrows409() {
        Reservation reservation = reservation("RES-X", ReservationStatus.PENDING,
                OffsetDateTime.now().minusMinutes(1));
        when(reservationRepository.findByReservationNumber("RES-X")).thenReturn(Optional.of(reservation));

        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> reservationService.cancel("RES-X"));

        assertEquals(HttpStatus.CONFLICT, e.getStatusCode());
    }

    @Test
    void cancelAlreadyCancelledThrows409() {
        Reservation reservation = reservation("RES-X", ReservationStatus.CANCELLED,
                OffsetDateTime.now().plusMinutes(30));
        when(reservationRepository.findByReservationNumber("RES-X")).thenReturn(Optional.of(reservation));

        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> reservationService.cancel("RES-X"));

        assertEquals(HttpStatus.CONFLICT, e.getStatusCode());
    }
}
