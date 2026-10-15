package com.coe.b04.server.service;

import com.coe.b04.server.enums.ReservationStatus;
import com.coe.b04.server.io.BookingResponse;
import com.coe.b04.server.io.ReservationResponse;
import com.coe.b04.server.model.Booking;
import com.coe.b04.server.repository.BookingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BookingServiceTest {

    private BookingService bookingService;
    private BookingRepository bookingRepository;
    private ReservationService reservationService;

    @BeforeEach
    void setUp() {
        bookingRepository = mock(BookingRepository.class);
        reservationService = mock(ReservationService.class);
        bookingService = new BookingService(bookingRepository, reservationService);
    }

    @Test
    void getByBookingNumberReturnsBookingWithReservationSnapshot() {
        Booking booking = Booking.builder()
                .bookingNumber("BOOK-20261015-ABC123")
                .reservationNumber("RES-X")
                .paidAt(OffsetDateTime.now())
                .createdAt(OffsetDateTime.now())
                .build();
        when(bookingRepository.findByBookingNumber("BOOK-20261015-ABC123"))
                .thenReturn(Optional.of(booking));
        when(reservationService.getByNumber("RES-X"))
                .thenReturn(ReservationResponse.builder()
                        .reservationNumber("RES-X")
                        .status(ReservationStatus.PAID)
                        .build());

        BookingResponse response = bookingService.getByBookingNumber("BOOK-20261015-ABC123");

        assertEquals("BOOK-20261015-ABC123", response.getBookingNumber());
        assertEquals("RES-X", response.getReservationNumber());
        assertEquals(booking.getPaidAt(), response.getPaidAt());
        assertEquals(ReservationStatus.PAID, response.getReservation().getStatus());
    }

    @Test
    void getByUnknownBookingNumberThrows404() {
        when(bookingRepository.findByBookingNumber("BOOK-UNKNOWN")).thenReturn(Optional.empty());

        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> bookingService.getByBookingNumber("BOOK-UNKNOWN"));

        assertEquals(HttpStatus.NOT_FOUND, e.getStatusCode());
    }
}
