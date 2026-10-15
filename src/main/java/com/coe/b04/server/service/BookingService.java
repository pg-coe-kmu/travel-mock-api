package com.coe.b04.server.service;

import com.coe.b04.server.io.BookingResponse;
import com.coe.b04.server.io.ReservationResponse;
import com.coe.b04.server.model.Booking;
import com.coe.b04.server.repository.BookingRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/*
 * Booking-Snapshot: Buchungskopf (bookingNumber, paidAt, createdAt) plus
 * der eingebettete Reservation-Snapshot. Der Reservation-Anteil kommt
 * wiederverwendet aus dem ReservationService.
 */
@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final ReservationService reservationService;

    public BookingService(BookingRepository bookingRepository, ReservationService reservationService) {
        this.bookingRepository = bookingRepository;
        this.reservationService = reservationService;
    }

    public BookingResponse getByBookingNumber(String bookingNumber) {
        Booking booking = bookingRepository.findByBookingNumber(bookingNumber)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No booking found for bookingNumber: " + bookingNumber));

        ReservationResponse reservation = reservationService.getByNumber(booking.getReservationNumber());

        return BookingResponse.builder()
                .bookingNumber(booking.getBookingNumber())
                .reservationNumber(booking.getReservationNumber())
                .paidAt(booking.getPaidAt())
                .createdAt(booking.getCreatedAt())
                .reservation(reservation)
                .build();
    }
}
