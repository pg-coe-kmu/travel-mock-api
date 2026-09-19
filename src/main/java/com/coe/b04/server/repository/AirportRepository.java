package com.coe.b04.server.repository;

import com.coe.b04.server.model.Airport;
import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Repository;

import java.util.List;

/*
 * Bei konfigurierter DB kommt die Location->IATA-Zuordnung direkt aus der
 * airports-Tabelle; ohne DB-Konfiguration dient die Bootstrap-Liste als
 * Fallback.
 */
@Repository
@Setter
@Getter
public class AirportRepository {

    private final CatalogQueryRepository catalogQueryRepository;

    // Fallback-Daten aus den JSON-Dateien (Bootstrap), nur ohne DB-Konfiguration
    List<Airport> airports;

    public AirportRepository(CatalogQueryRepository catalogQueryRepository) {
        this.catalogQueryRepository = catalogQueryRepository;
    }

    public String getAirportIataCodeByLocation(String location) {
        if (catalogQueryRepository.isDbConfigured()) {
            return catalogQueryRepository.findAirportIataCodeByLocation(location);
        }
        return airports.stream()
                .filter(airport -> airport.getLocation().equalsIgnoreCase(location))
                .map(Airport::getIata_code)
                .findFirst()
                .orElse(null);
    }

}
