package com.coe.b04.server.repository;

import com.coe.b04.server.model.Booking;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

/*
 * JDBC-Zugriff auf die Booking-Tabelle (Schema: db/bookings.sql).
 * Der Zahlungsuebergang PENDING -> PAID + Booking-Insert laufen atomar
 * in einer Transaktion; der bedingte UPDATE verliert gegen concurrente
 * Expiry/Cancel/Pay-Versuche und liefert dann false.
 */
@Repository
public class BookingRepository {

    private final JdbcClient jdbcClient;

    public BookingRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    /**
     * Zahlung: setzt die Reservation atomar auf PAID (nur solange laut
     * DB-Uhr noch PENDING und nicht abgelaufen) und legt die Booking an.
     * Rueckgabe false = kein Row getroffen (abgelaufen/cancelled/schon bezahlt).
     * Die gehaltene Availability bleibt dabei reserviert (kein Release).
     */
    @Transactional
    public boolean pay(UUID reservationId, String bookingNumber, OffsetDateTime paidAt) {
        int rows = jdbcClient.sql("""
                        update reservations
                        set status = 'PAID'
                        where id = ? and status = 'PENDING'
                          and expires_at > CURRENT_TIMESTAMP
                        """)
                .param(reservationId)
                .update();
        if (rows == 0) {
            return false;
        }
        jdbcClient.sql("""
                        insert into bookings (booking_number, reservation_id, paid_at)
                        values (?, ?, ?)
                        """)
                .param(bookingNumber)
                .param(reservationId)
                .param(paidAt)
                .update();
        return true;
    }

    public boolean existsByBookingNumber(String bookingNumber) {
        return jdbcClient.sql("select count(*) from bookings where booking_number = ?")
                .param(bookingNumber)
                .query(Long.class)
                .single() > 0;
    }

    public Optional<Booking> findByBookingNumber(String bookingNumber) {
        return jdbcClient.sql("""
                        select b.id, b.booking_number, b.reservation_id,
                               r.reservation_number, b.paid_at, b.created_at
                        from bookings b
                        join reservations r on r.id = b.reservation_id
                        where b.booking_number = ?
                        """)
                .param(bookingNumber)
                .query((rs, rowNum) -> Booking.builder()
                        .id(rs.getObject("id", UUID.class))
                        .bookingNumber(rs.getString("booking_number"))
                        .reservationId(rs.getObject("reservation_id", UUID.class))
                        .reservationNumber(rs.getString("reservation_number"))
                        .paidAt(rs.getObject("paid_at", OffsetDateTime.class))
                        .createdAt(rs.getObject("created_at", OffsetDateTime.class))
                        .build())
                .optional();
    }
}
