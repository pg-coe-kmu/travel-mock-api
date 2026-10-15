package com.coe.b04.server.service;

import com.coe.b04.server.enums.ReservationStatus;
import com.coe.b04.server.io.PaymentRequest;
import com.coe.b04.server.io.PaymentResponse;
import com.coe.b04.server.model.Reservation;
import com.coe.b04.server.repository.BookingRepository;
import com.coe.b04.server.repository.ReservationRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

/*
 * Zahlung: wandelt eine gueltige PENDING-Reservation (innerhalb der
 * 30 Minuten, laut DB-Uhr) in eine Booking um (Status PAID).
 *
 * - success=false -> 402, Reservation bleibt unveraendert.
 * - Der atomare Uebergang (PENDING -> PAID + Booking-Insert) verliert
 *   gegen concurrente Expiry/Cancel/Pay-Versuche -> 409.
 * - Eine PAID-Reservation gibt ihre Availability nie zurueck: Release
 *   laeuft nur ueber PENDING -> EXPIRED/CANCELLED.
 */
@Service
public class PaymentService {

    // Alphabet ohne I, O, 0, 1 (Verwechslungsgefahr beim Ablesen) -
    // gleiches Schema wie die Reservation-Nummern
    private static final String NUMBER_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int NUMBER_RANDOM_LENGTH = 6;
    private static final DateTimeFormatter NUMBER_DATE_FORMAT = DateTimeFormatter.BASIC_ISO_DATE;

    private final ReservationRepository reservationRepository;
    private final BookingRepository bookingRepository;
    private final SecureRandom random = new SecureRandom();

    public PaymentService(ReservationRepository reservationRepository,
                          BookingRepository bookingRepository) {
        this.reservationRepository = reservationRepository;
        this.bookingRepository = bookingRepository;
    }

    public PaymentResponse pay(PaymentRequest request) {
        Reservation reservation = applyLazyExpiry(findOrThrow(request.getReservationNumber()));

        if (Boolean.FALSE.equals(request.getSuccess())) {
            throw new ResponseStatusException(HttpStatus.PAYMENT_REQUIRED,
                    "Payment failed, reservation unchanged");
        }
        if (reservation.getStatus() != ReservationStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Reservation cannot be paid, status: " + reservation.getStatus());
        }

        OffsetDateTime paidAt = OffsetDateTime.now();
        String bookingNumber = generateBookingNumber();
        // Atomarer Uebergang in der DB (nur solange laut DB-Uhr noch PENDING
        // und nicht abgelaufen) - verliert gegen concurrente Aenderungen
        if (!bookingRepository.pay(reservation.getId(), bookingNumber, paidAt)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Reservation cannot be paid: expired, already paid or cancelled");
        }
        reservation.setStatus(ReservationStatus.PAID);

        return PaymentResponse.builder()
                .bookingNumber(bookingNumber)
                .reservationNumber(reservation.getReservationNumber())
                .status(ReservationStatus.PAID)
                .paidAt(paidAt)
                .totalPrice(reservation.getTotalPrice())
                .currency(reservation.getCurrency())
                .build();
    }

    private Reservation findOrThrow(String reservationNumber) {
        return reservationRepository.findByReservationNumber(reservationNumber)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No reservation found for reservationNumber: " + reservationNumber));
    }

    /*
     * Abgelaufene PENDING-Reservations werden beim Zahlversuch wie beim
     * Cancel zunaechst lazy auf EXPIRED gesetzt (inkl. Availability-Release).
     * Bezahlt werden kann dann nur noch, was laut App-Uhr nicht abgelaufen
     * ist; die endgueltige Entscheidung trifft die DB-Uhr im atomaren UPDATE.
     */
    private Reservation applyLazyExpiry(Reservation reservation) {
        if (reservation.getStatus() == ReservationStatus.PENDING
                && !reservation.getExpiresAt().isAfter(OffsetDateTime.now())) {
            if (reservationRepository.expireAndRelease(reservation.getId())) {
                reservation.setStatus(ReservationStatus.EXPIRED);
            } else {
                return findOrThrow(reservation.getReservationNumber());
            }
        }
        return reservation;
    }

    private String generateBookingNumber() {
        for (int attempt = 0; attempt < 10; attempt++) {
            StringBuilder suffix = new StringBuilder(NUMBER_RANDOM_LENGTH);
            for (int i = 0; i < NUMBER_RANDOM_LENGTH; i++) {
                suffix.append(NUMBER_ALPHABET.charAt(random.nextInt(NUMBER_ALPHABET.length())));
            }
            String number = "BOOK-" + NUMBER_DATE_FORMAT.format(LocalDate.now()) + "-" + suffix;
            if (!bookingRepository.existsByBookingNumber(number)) {
                return number;
            }
        }
        throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                "Could not generate a unique booking number");
    }
}
