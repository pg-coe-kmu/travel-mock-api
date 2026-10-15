package com.coe.b04.server.service;

import com.coe.b04.server.enums.ReservationStatus;
import com.coe.b04.server.io.BookingResponse;
import com.coe.b04.server.io.ReservationResponse;
import com.coe.b04.server.model.Booking;
import com.coe.b04.server.model.CancellationDeadline;
import com.coe.b04.server.model.CancellationPolicy;
import com.coe.b04.server.model.Reservation;
import com.coe.b04.server.model.ReservationItem;
import com.coe.b04.server.repository.BookingRepository;
import com.coe.b04.server.repository.CatalogQueryRepository;
import com.coe.b04.server.repository.ReservationRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

/*
 * Booking-Snapshot + Stornierung.
 *
 * Stornierung beruecksichtigt die cancellationPolicy der Items:
 *  - Fluege haben keine Policy und blockieren nie.
 *  - Rooms/Cars: free_cancellation + Deadline
 *      AFTER_RESERVATION  -> relativ zum paidAt der Booking
 *      BEFORE_CHECK_IN    -> relativ zum check_in (Mitternacht UTC)
 *      BEFORE_PICKUP      -> relativ zum pickup_at
 *  - Verletzt ein Item die Policy, ist die ganze Booking nicht stornierbar
 *    (409 mit Nennung des Items).
 *
 * Erfolgreiche Stornierung: atomar PAID -> CANCELLED, bookings.cancelled_at
 * und Availability-Release (genau einmal, Claim-Guard) in einer Transaktion.
 */
@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final ReservationRepository reservationRepository;
    private final CatalogQueryRepository catalogQueryRepository;
    private final ReservationService reservationService;

    public BookingService(BookingRepository bookingRepository,
                          ReservationRepository reservationRepository,
                          CatalogQueryRepository catalogQueryRepository,
                          ReservationService reservationService) {
        this.bookingRepository = bookingRepository;
        this.reservationRepository = reservationRepository;
        this.catalogQueryRepository = catalogQueryRepository;
        this.reservationService = reservationService;
    }

    public BookingResponse getByBookingNumber(String bookingNumber) {
        Booking booking = requireBooking(bookingNumber);
        return toResponse(booking, reservationService.getByNumber(booking.getReservationNumber()));
    }

    public BookingResponse cancel(String bookingNumber) {
        Booking booking = requireBooking(bookingNumber);
        if (booking.getCancelledAt() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Booking already cancelled");
        }
        Reservation reservation = reservationRepository
                .findByReservationNumber(booking.getReservationNumber())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No reservation found for reservationNumber: " + booking.getReservationNumber()));
        if (reservation.getStatus() != ReservationStatus.PAID) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Booking cannot be cancelled, reservation status: " + reservation.getStatus());
        }

        validateCancellationPolicy(reservation, booking.getPaidAt());

        // Atomare Stornierung in der DB; verliert gegen concurrente Aenderungen
        OffsetDateTime cancelledAt = OffsetDateTime.now();
        if (!bookingRepository.cancel(reservation.getId(), cancelledAt)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Booking was modified concurrently and cannot be cancelled");
        }
        booking.setCancelledAt(cancelledAt);
        reservation.setStatus(ReservationStatus.CANCELLED);
        reservation.setCancelledAt(cancelledAt);

        return toResponse(booking, reservationService.getByNumber(booking.getReservationNumber()));
    }

    private Booking requireBooking(String bookingNumber) {
        return bookingRepository.findByBookingNumber(bookingNumber)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No booking found for bookingNumber: " + bookingNumber));
    }

    private BookingResponse toResponse(Booking booking, ReservationResponse reservation) {
        return BookingResponse.builder()
                .bookingNumber(booking.getBookingNumber())
                .reservationNumber(booking.getReservationNumber())
                .paidAt(booking.getPaidAt())
                .createdAt(booking.getCreatedAt())
                .cancelledAt(booking.getCancelledAt())
                .reservation(reservation)
                .build();
    }

    /*
     * Prueft die Policy jedes policyfaehigen Items (Room/Car). Fluege haben
     * keine Policy und blockieren nie. Das erste verletzende Item bricht mit
     * 409 ab.
     */
    private void validateCancellationPolicy(Reservation reservation, OffsetDateTime paidAt) {
        OffsetDateTime now = OffsetDateTime.now();
        for (ReservationItem item : reservation.getItems()) {
            switch (item.getItemType()) {
                case HOTEL -> checkPolicy(
                        catalogQueryRepository.roomCancellationPolicy(item.getHotel().getRoomId()),
                        item.getHotel().getCheckIn().atStartOfDay().atOffset(ZoneOffset.UTC),
                        "room " + item.getHotel().getRoomId(), now, paidAt);
                case CAR -> checkPolicy(
                        catalogQueryRepository.carCancellationPolicy(item.getCar().getCarId()),
                        item.getCar().getPickupAt(),
                        "car " + item.getCar().getCarId(), now, paidAt);
                case FLIGHT -> { /* keine Policy -> blockiert nie */ }
            }
        }
    }

    private void checkPolicy(Optional<CancellationPolicy> policy, OffsetDateTime start,
                             String itemLabel, OffsetDateTime now, OffsetDateTime paidAt) {
        if (policy.isEmpty() || !policy.get().isFreeCancellation()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Booking cannot be cancelled: " + itemLabel + " does not allow free cancellation");
        }
        CancellationDeadline deadline = policy.get().getCancellationDeadline();
        if (deadline == null) {
            return; // free, ohne Deadline -> immer stornierbar
        }
        ChronoUnit unit = switch (deadline.getUnit()) {
            case "HOURS" -> ChronoUnit.HOURS;
            case "DAYS" -> ChronoUnit.DAYS;
            default -> null;
        };
        OffsetDateTime deadlineAt = switch (deadline.getType()) {
            case "AFTER_RESERVATION" ->
                    unit == null ? null : paidAt.plus(deadline.getValue(), unit);
            case "BEFORE_CHECK_IN", "BEFORE_PICKUP" ->
                    unit == null ? null : start.minus(deadline.getValue(), unit);
            default -> null;
        };
        if (deadlineAt == null || now.isAfter(deadlineAt)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Booking cannot be cancelled: cancellation deadline for " + itemLabel + " has passed");
        }
    }
}
