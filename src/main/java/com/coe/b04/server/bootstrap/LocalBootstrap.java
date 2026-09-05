package com.coe.b04.server.bootstrap;

import com.coe.b04.server.loader.Loader;
import com.coe.b04.server.reader.LocalReader;
import com.coe.b04.server.repository.AirportRepository;
import com.coe.b04.server.repository.CarRepository;
import com.coe.b04.server.repository.FlightRepository;
import com.coe.b04.server.repository.HotelRepository;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/*
 * Writes the mock data provided by the Loader into the repositories
 * (local profile, only local files from the /data folder).
 */
@Profile("local")
@Slf4j
@Component
@DependsOn("envConfig")
public class LocalBootstrap extends Bootstrap {

    public LocalBootstrap(HotelRepository hotelRepository, FlightRepository flightRepository,
                          CarRepository carRepository, AirportRepository airportRepository) {
        super(hotelRepository, flightRepository, carRepository, airportRepository);
    }

    @PostConstruct
    public void init() {
        log.info("Loading data for local...");

        Loader loader = new Loader(new LocalReader());
        bootstrapData(loader);
        logBootstrapSummary();
    }
}
