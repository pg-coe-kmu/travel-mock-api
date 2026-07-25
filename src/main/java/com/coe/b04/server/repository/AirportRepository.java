package com.coe.b04.server.repository;

import com.coe.b04.server.model.Airport;
import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@Setter
@Getter
public class AirportRepository {
    List<Airport> airports;

    public String getAirportIataCodeByLocation(String location) {
        return airports.stream()
                .filter(airport -> airport.getLocation().equalsIgnoreCase(location))
                .map(Airport::getIata_code)
                .findFirst()
                .orElse(null);
    }

}
