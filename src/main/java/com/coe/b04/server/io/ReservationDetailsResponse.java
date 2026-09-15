package com.coe.b04.server.io;

import com.coe.b04.server.model.CarProvider;
import com.coe.b04.server.model.Flight;
import com.coe.b04.server.model.Hotel;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * GET /reservation/details - wie ReservationResponse, zusaetzlich die
 * vollen Angebotsinhalte, aufgeloest ueber die Mock-APIs:
 *  - flight.outbound / flight.return : vollstaendige Fluege
 *  - hotel                           : Hotel, gefiltert auf das reservierte Zimmer
 *  - car                             : Provider, gefiltert auf das reservierte Car
 * Nicht reservierte Leistungen werden weggelassen (NON_NULL).
 */
@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ReservationDetailsResponse extends ReservationResponse {

    private FlightDetails flight;
    private Hotel hotel;
    private CarProvider car;

    public static ReservationDetailsResponse from(ReservationResponse base,
                                                  FlightDetails flight, Hotel hotel, CarProvider car) {
        ReservationDetailsResponse details = new ReservationDetailsResponse();
        details.setReservationNumber(base.getReservationNumber());
        details.setStatus(base.getStatus());
        details.setCreatedAt(base.getCreatedAt());
        details.setExpiresAt(base.getExpiresAt());
        details.setExpiresInSeconds(base.getExpiresInSeconds());
        details.setTrip(base.getTrip());
        details.setItems(base.getItems());
        details.setPrice(base.getPrice());
        details.setFlight(flight);
        details.setHotel(hotel);
        details.setCar(car);
        return details;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FlightDetails {
        private Flight outbound;
        private Flight returnFlight;
    }
}
