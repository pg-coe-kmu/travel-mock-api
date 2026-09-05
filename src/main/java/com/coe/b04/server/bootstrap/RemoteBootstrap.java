package com.coe.b04.server.bootstrap;

import com.coe.b04.server.loader.Loader;
import com.coe.b04.server.reader.S3Reader;
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
 * (remote profile, always S3).
 */
@Profile("remote")
@Slf4j
@Component
@DependsOn("envConfig")
public class RemoteBootstrap extends Bootstrap {

    private final S3Reader s3Reader;

    public RemoteBootstrap(HotelRepository hotelRepository, FlightRepository flightRepository,
                           CarRepository carRepository, AirportRepository airportRepository,
                           S3Reader s3Reader) {
        super(hotelRepository, flightRepository, carRepository, airportRepository);

        this.s3Reader = s3Reader;
    }

    @PostConstruct
    public void init() {
        log.info("Loading data from S3...");

        Loader loader = new Loader(s3Reader);
        bootstrapData(loader);
        logBootstrapSummary();
    }
}
