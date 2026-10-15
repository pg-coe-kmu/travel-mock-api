package com.coe.b04.server.service;

import com.coe.b04.server.enums.ReservationStatus;
import com.coe.b04.server.io.PaymentRequest;
import com.coe.b04.server.io.PaymentResponse;
import com.coe.b04.server.model.Reservation;
import com.coe.b04.server.repository.BookingRepository;
import com.coe.b04.server.repository.ReservationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PaymentServiceTest {

    private PaymentService paymentService;
    private ReservationRepository reservationRepository;
    private BookingRepository bookingRepository;

    @BeforeEach
    void setUp() {
        reservationRepository = mock(ReservationRepository.class);
        bookingRepository = mock(BookingRepository.class);
        paymentService = new PaymentService(reservationRepository, bookingRepository);
    }

    private Reservation reservation(ReservationStatus status, OffsetDateTime expiresAt) {
        return Reservation.builder()
                .id(UUID.randomUUID())
                .reservationNumber("RES-X")
                .status(status)
                .currency("EUR")
                .totalPrice(new BigDecimal("1543.50"))
                .createdAt(OffsetDateTime.now().minusMinutes(5))
                .expiresAt(expiresAt)
                .build();
    }

    private PaymentRequest request(boolean success) {
        PaymentRequest request = new PaymentRequest();
        request.setReservationNumber("RES-X");
        request.setSuccess(success);
        return request;
    }

    @Test
    void paySuccessCreatesBookingWithBookingNumber() {
        when(reservationRepository.findByReservationNumber("RES-X"))
                .thenReturn(Optional.of(reservation(ReservationStatus.PENDING,
                        OffsetDateTime.now().plusMinutes(30))));
        when(bookingRepository.pay(any(), anyString(), any())).thenReturn(true);

        PaymentResponse response = paymentService.pay(request(true));

        assertEquals(ReservationStatus.PAID, response.getStatus());
        assertEquals("RES-X", response.getReservationNumber());
        assertTrue(response.getBookingNumber().matches("BOOK-\\d{8}-[A-HJ-NP-Z2-9]{6}"));
        assertEquals(0, new BigDecimal("1543.50").compareTo(response.getTotalPrice()));
        assertEquals("EUR", response.getCurrency());
        verify(bookingRepository).pay(any(), anyString(), any());
    }

    @Test
    void failedPaymentReturns402AndLeavesReservationUntouched() {
        when(reservationRepository.findByReservationNumber("RES-X"))
                .thenReturn(Optional.of(reservation(ReservationStatus.PENDING,
                        OffsetDateTime.now().plusMinutes(30))));

        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> paymentService.pay(request(false)));

        assertEquals(HttpStatus.PAYMENT_REQUIRED, e.getStatusCode());
        verify(bookingRepository, never()).pay(any(), anyString(), any());
    }

    @Test
    void payUnknownReservationThrows404() {
        when(reservationRepository.findByReservationNumber("RES-X")).thenReturn(Optional.empty());

        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> paymentService.pay(request(true)));

        assertEquals(HttpStatus.NOT_FOUND, e.getStatusCode());
    }

    @Test
    void payExpiredReservationThrows409() {
        when(reservationRepository.findByReservationNumber("RES-X"))
                .thenReturn(Optional.of(reservation(ReservationStatus.PENDING,
                        OffsetDateTime.now().minusMinutes(1))));
        when(reservationRepository.expireAndRelease(any())).thenReturn(true);

        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> paymentService.pay(request(true)));

        assertEquals(HttpStatus.CONFLICT, e.getStatusCode());
        verify(reservationRepository).expireAndRelease(any());
        verify(bookingRepository, never()).pay(any(), anyString(), any());
    }

    @Test
    void payCancelledReservationThrows409() {
        when(reservationRepository.findByReservationNumber("RES-X"))
                .thenReturn(Optional.of(reservation(ReservationStatus.CANCELLED,
                        OffsetDateTime.now().plusMinutes(30))));

        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> paymentService.pay(request(true)));

        assertEquals(HttpStatus.CONFLICT, e.getStatusCode());
        verify(bookingRepository, never()).pay(any(), anyString(), any());
    }

    @Test
    void payLosesRaceThrows409() {
        when(reservationRepository.findByReservationNumber("RES-X"))
                .thenReturn(Optional.of(reservation(ReservationStatus.PENDING,
                        OffsetDateTime.now().plusMinutes(30))));
        when(bookingRepository.pay(any(), anyString(), any())).thenReturn(false);

        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> paymentService.pay(request(true)));

        assertEquals(HttpStatus.CONFLICT, e.getStatusCode());
    }

    @Test
    void payRetriesOnBookingNumberCollision() {
        when(reservationRepository.findByReservationNumber("RES-X"))
                .thenReturn(Optional.of(reservation(ReservationStatus.PENDING,
                        OffsetDateTime.now().plusMinutes(30))));
        when(bookingRepository.existsByBookingNumber(anyString()))
                .thenReturn(true)
                .thenReturn(false);
        when(bookingRepository.pay(any(), anyString(), any())).thenReturn(true);

        PaymentResponse response = paymentService.pay(request(true));

        assertTrue(response.getBookingNumber().matches("BOOK-\\d{8}-[A-HJ-NP-Z2-9]{6}"));
        // erster Versuch kollidiert (true), zweiter ist frei (false)
        verify(bookingRepository, times(2)).existsByBookingNumber(anyString());
    }
}
