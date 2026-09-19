package com.coe.b04.server.repository;

import org.springframework.stereotype.Repository;

/*
 * Location->IATA-Zuordnung kommt ausschliesslich aus der airports-Tabelle.
 */
@Repository
public class AirportRepository {

    private final CatalogQueryRepository catalogQueryRepository;

    public AirportRepository(CatalogQueryRepository catalogQueryRepository) {
        this.catalogQueryRepository = catalogQueryRepository;
    }

    public String getAirportIataCodeByLocation(String location) {
        return catalogQueryRepository.findAirportIataCodeByLocation(location);
    }

}
