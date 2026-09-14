package com.coe.b04.server.repository;

import com.coe.b04.server.enums.ReservationStatus;
import com.coe.b04.server.enums.ServiceType;
import com.coe.b04.server.model.Reservation;
import com.coe.b04.server.model.ReservationItem;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * JDBC-Zugriff auf Supabase/PostgreSQL (Schema: db/reservations.sql).
 * Verbindungsdetails baut DataSourceConfig aus SUPABASE_URL +
 * SUPABASE_DB_PASSWORD; ohne konfigurierte DB verbindet Hikari erst
 * beim ersten Zugriff - die uebrigen Mock-Endpoints bleiben lauffaehig.
 */
@Repository
public class ReservationRepository {

    private final JdbcClient jdbcClient;

    public ReservationRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    /**
     * Legt Kopf + Leistungen in einer Transaktion an. createdAt/expiresAt
     * setzt der Aufrufer (App-Uhr = Garantie fuer expiresAt = createdAt + 30 min).
     */
    @Transactional
    public Reservation save(Reservation reservation) {
        UUID id = jdbcClient.sql("""
                        insert into reservations (
                            reservation_number, status, origin, destination,
                            departure_date, return_date, adults, children, infants,
                            currency, total_price, created_at, expires_at, cancelled_at)
                        values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        returning id
                        """)
                .param(reservation.getReservationNumber())
                .param(reservation.getStatus().name())
                .param(reservation.getOrigin())
                .param(reservation.getDestination())
                .param(reservation.getDepartureDate())
                .param(reservation.getReturnDate())
                .param(reservation.getAdults())
                .param(reservation.getChildren())
                .param(reservation.getInfants())
                .param(reservation.getCurrency())
                .param(reservation.getTotalPrice())
                .param(reservation.getCreatedAt())
                .param(reservation.getExpiresAt())
                .param(reservation.getCancelledAt())
                .query(UUID.class)
                .single();
        reservation.setId(id);

        for (ReservationItem item : reservation.getServices()) {
            jdbcClient.sql("""
                            insert into reservation_services (
                                reservation_id, service_type, service_id, provider_id,
                                price, check_in, check_out, room_id,
                                pickup_date, return_date, pickup_location, return_location)
                            values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                            """)
                    .param(id)
                    .param(item.getServiceType().name())
                    .param(item.getServiceId())
                    .param(item.getProviderId())
                    .param(item.getPrice())
                    .param(item.getCheckIn())
                    .param(item.getCheckOut())
                    .param(item.getRoomId())
                    .param(item.getPickupDate())
                    .param(item.getReturnDate())
                    .param(item.getPickupLocation())
                    .param(item.getReturnLocation())
                    .update();
        }
        return reservation;
    }

    public Optional<Reservation> findByReservationNumber(String reservationNumber) {
        Optional<Reservation> head = jdbcClient.sql("""
                        select id, reservation_number, status, origin, destination,
                               departure_date, return_date, adults, children, infants,
                               currency, total_price, created_at, expires_at, cancelled_at
                        from reservations
                        where reservation_number = ?
                        """)
                .param(reservationNumber)
                .query((rs, rowNum) -> Reservation.builder()
                        .id(rs.getObject("id", UUID.class))
                        .reservationNumber(rs.getString("reservation_number"))
                        .status(ReservationStatus.valueOf(rs.getString("status")))
                        .origin(rs.getString("origin"))
                        .destination(rs.getString("destination"))
                        .departureDate(rs.getObject("departure_date", LocalDate.class))
                        .returnDate(rs.getObject("return_date", LocalDate.class))
                        .adults(rs.getInt("adults"))
                        .children(rs.getInt("children"))
                        .infants(rs.getInt("infants"))
                        .currency(rs.getString("currency"))
                        .totalPrice(rs.getBigDecimal("total_price"))
                        .createdAt(rs.getObject("created_at", OffsetDateTime.class))
                        .expiresAt(rs.getObject("expires_at", OffsetDateTime.class))
                        .cancelledAt(rs.getObject("cancelled_at", OffsetDateTime.class))
                        .build())
                .optional();

        if (head.isEmpty()) {
            return head;
        }
        List<ReservationItem> services = jdbcClient.sql("""
                        select id, service_type, service_id, provider_id, price,
                               check_in, check_out, room_id,
                               pickup_date, return_date, pickup_location, return_location
                        from reservation_services
                        where reservation_id = ?
                        order by service_type
                        """)
                .param(head.get().getId())
                .query((rs, rowNum) -> ReservationItem.builder()
                        .id(rs.getObject("id", UUID.class))
                        .serviceType(ServiceType.valueOf(rs.getString("service_type")))
                        .serviceId(rs.getString("service_id"))
                        .providerId(rs.getString("provider_id"))
                        .price(rs.getBigDecimal("price"))
                        .checkIn(rs.getObject("check_in", LocalDate.class))
                        .checkOut(rs.getObject("check_out", LocalDate.class))
                        .roomId(rs.getString("room_id"))
                        .pickupDate(rs.getObject("pickup_date", LocalDate.class))
                        .returnDate(rs.getObject("return_date", LocalDate.class))
                        .pickupLocation(rs.getString("pickup_location"))
                        .returnLocation(rs.getString("return_location"))
                        .build())
                .list();
        head.get().setServices(services);
        return head;
    }

    public void updateStatus(UUID id, ReservationStatus status, OffsetDateTime cancelledAt) {
        jdbcClient.sql("update reservations set status = ?, cancelled_at = ? where id = ?")
                .param(status.name())
                .param(cancelledAt)
                .param(id)
                .update();
    }
}
