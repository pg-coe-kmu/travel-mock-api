package com.coe.b04.server.service;

import com.coe.b04.server.enums.ReservationStatus;
import com.coe.b04.server.enums.ServiceType;
import com.coe.b04.server.io.BookingResponse;
import com.coe.b04.server.io.ReservationResponse;
import com.coe.b04.server.model.Booking;
import com.coe.b04.server.model.CancellationDeadline;
import com.coe.b04.server.model.CancellationPolicy;
import com.coe.b04.server.model.Reservation;
import com.coe.b04.server.model.ReservationHotelDetail;
import com.coe.b04.server.model.ReservationItem;
import com.coe.b04.server.repository.BookingRepository;
import com.coe.b04.server.repository.CatalogQueryRepository;
import com.coe.b04.server.repository.ReservationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BookingServiceTest {

    private BookingService bookingService;
    private BookingRepository bookingRepository;
    private ReservationRepository reservationRepository;
    private CatalogQueryRepository catalogQueryRepository;
    private ReservationService reservationService;

    @BeforeEach
    void setUp() {
        bookingRepository = mock(BookingRepository.class);
        reservationRepository = mock(ReservationRepository.class);
        catalogQueryRepository = mock(CatalogQueryRepository.class);
        reservationService = mock(ReservationService.class);
        bookingService = new BookingService(bookingRepository, reservationRepository,
                catalogQueryRepository, reservationService);
    }

    private Booking booking(OffsetDateTime cancelledAt) {
        return Booking.builder()
                .id(UUID.randomUUID())
                .bookingNumber("BOOK-X")
                .reservationId(UUID.randomUUID())
                .reservationNumber("RES-X")
                .paidAt(OffsetDateTime.now().minusMinutes(10))
                .createdAt(OffsetDateTime.now().minusMinutes(10))
                .cancelledAt(cancelledAt)
                .build();
    }

    private Reservation reservation(ReservationStatus status) {
        ReservationItem hotel = ReservationItem.builder()
                .itemType(ServiceType.HOTEL)
                .hotel(ReservationHotelDetail.builder()
                        .hotelId("HOT-1").roomId("ROOM-1")
                        .checkIn(LocalDate.now().plusDays(5))
                        .checkOut(LocalDate.now().plusDays(6))
                        .hotelName("Test Hotel").roomName("Standard Double")
                        .build())
                .build();
        return Reservation.builder()
                .id(UUID.randomUUID())
                .reservationNumber("RES-X")
                .status(status)
                .expiresAt(OffsetDateTime.now().plusMinutes(30))
                .items(List.of(hotel))
                .build();
    }

    private CancellationPolicy freePolicy(String type, int value, String unit) {
        return new CancellationPolicy(true, new CancellationDeadline(type, value, unit));
    }

    private void stubBookingAndReservation(Booking booking, Reservation reservation) {
        when(bookingRepository.findByBookingNumber("BOOK-X")).thenReturn(Optional.of(booking));
        when(reservationRepository.findByReservationNumber("RES-X")).thenReturn(Optional.of(reservation));
    }

    // ---------- Snapshot ----------

    @Test
    void getByBookingNumberReturnsBookingWithReservationSnapshot() {
        stubBookingAndReservation(booking(null), reservation(ReservationStatus.PAID));
        when(reservationService.getByNumber("RES-X"))
                .thenReturn(ReservationResponse.builder()
                        .reservationNumber("RES-X")
                        .status(ReservationStatus.PAID)
                        .build());

        BookingResponse response = bookingService.getByBookingNumber("BOOK-X");

        assertEquals("BOOK-X", response.getBookingNumber());
        assertEquals("RES-X", response.getReservationNumber());
        assertEquals(ReservationStatus.PAID, response.getReservation().getStatus());
        assertNull(response.getCancelledAt());
    }

    @Test
    void getByUnknownBookingNumberThrows404() {
        when(bookingRepository.findByBookingNumber("BOOK-UNKNOWN")).thenReturn(Optional.empty());

        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> bookingService.getByBookingNumber("BOOK-UNKNOWN"));

        assertEquals(HttpStatus.NOT_FOUND, e.getStatusCode());
    }

    // ---------- Cancel ----------

    @Test
    void cancelSucceedsWithFreePolicyAndFutureDeadline() {
        stubBookingAndReservation(booking(null), reservation(ReservationStatus.PAID));
        when(catalogQueryRepository.roomCancellationPolicy("ROOM-1"))
                .thenReturn(Optional.of(freePolicy("BEFORE_CHECK_IN", 24, "HOURS")));
        when(bookingRepository.cancel(any(), any())).thenReturn(true);
        when(reservationService.getByNumber("RES-X"))
                .thenReturn(ReservationResponse.builder()
                        .reservationNumber("RES-X")
                        .status(ReservationStatus.CANCELLED)
                        .build());

        BookingResponse response = bookingService.cancel("BOOK-X");

        assertNotNull(response.getCancelledAt());
        assertEquals(ReservationStatus.CANCELLED, response.getReservation().getStatus());
        verify(bookingRepository).cancel(any(), any());
    }

    @Test
    void cancelBlockedByNonFreePolicy() {
        stubBookingAndReservation(booking(null), reservation(ReservationStatus.PAID));
        when(catalogQueryRepository.roomCancellationPolicy("ROOM-1"))
                .thenReturn(Optional.of(new CancellationPolicy(false, null)));

        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> bookingService.cancel("BOOK-X"));

        assertEquals(HttpStatus.CONFLICT, e.getStatusCode());
        verify(bookingRepository, never()).cancel(any(), any());
    }

    @Test
    void cancelBlockedByPassedDeadline() {
        stubBookingAndReservation(booking(null), reservation(ReservationStatus.PAID));
        // check-in gestern, 24h-Frist vor Abflug -> laengst abgelaufen
        Reservation reservation = reservation(ReservationStatus.PAID);
        reservation.getItems().getFirst().getHotel()
                .setCheckIn(LocalDate.now().minusDays(1));
        when(reservationRepository.findByReservationNumber("RES-X")).thenReturn(Optional.of(reservation));
        when(catalogQueryRepository.roomCancellationPolicy("ROOM-1"))
                .thenReturn(Optional.of(freePolicy("BEFORE_CHECK_IN", 24, "HOURS")));

        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> bookingService.cancel("BOOK-X"));

        assertEquals(HttpStatus.CONFLICT, e.getStatusCode());
        verify(bookingRepository, never()).cancel(any(), any());
    }

    @Test
    void flightsWithoutPolicyDoNotBlockCancellation() {
        Reservation flightOnly = Reservation.builder()
                .id(UUID.randomUUID())
                .reservationNumber("RES-X")
                .status(ReservationStatus.PAID)
                .expiresAt(OffsetDateTime.now().plusMinutes(30))
                .items(List.of(ReservationItem.builder().itemType(ServiceType.FLIGHT).build()))
                .build();
        stubBookingAndReservation(booking(null), flightOnly);
        when(bookingRepository.cancel(any(), any())).thenReturn(true);
        when(reservationService.getByNumber("RES-X"))
                .thenReturn(ReservationResponse.builder().status(ReservationStatus.CANCELLED).build());

        BookingResponse response = bookingService.cancel("BOOK-X");

        assertEquals(ReservationStatus.CANCELLED, response.getReservation().getStatus());
        verify(catalogQueryRepository, never()).roomCancellationPolicy(anyString());
    }

    @Test
    void cancelAlreadyCancelledThrows409() {
        stubBookingAndReservation(booking(OffsetDateTime.now().minusMinutes(5)),
                reservation(ReservationStatus.CANCELLED));

        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> bookingService.cancel("BOOK-X"));

        assertEquals(HttpStatus.CONFLICT, e.getStatusCode());
        verify(bookingRepository, never()).cancel(any(), any());
    }

    @Test
    void cancelNotPaidReservationThrows409() {
        stubBookingAndReservation(booking(null), reservation(ReservationStatus.PENDING));

        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> bookingService.cancel("BOOK-X"));

        assertEquals(HttpStatus.CONFLICT, e.getStatusCode());
        verify(bookingRepository, never()).cancel(any(), any());
    }

    @Test
    void cancelLosesRaceThrows409() {
        stubBookingAndReservation(booking(null), reservation(ReservationStatus.PAID));
        when(catalogQueryRepository.roomCancellationPolicy("ROOM-1"))
                .thenReturn(Optional.of(freePolicy("AFTER_RESERVATION", 24, "HOURS")));
        when(bookingRepository.cancel(any(), any())).thenReturn(false);

        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> bookingService.cancel("BOOK-X"));

        assertEquals(HttpStatus.CONFLICT, e.getStatusCode());
    }
}
