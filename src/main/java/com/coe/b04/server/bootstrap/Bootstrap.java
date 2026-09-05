package com.coe.b04.server.bootstrap;

import com.coe.b04.server.loader.Loader;
import com.coe.b04.server.repository.AirportRepository;
import com.coe.b04.server.repository.CarRepository;
import com.coe.b04.server.repository.FlightRepository;
import com.coe.b04.server.repository.HotelRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class Bootstrap {

    protected final HotelRepository hotelRepository;
    protected final FlightRepository flightRepository;
    protected final CarRepository carRepository;
    protected final AirportRepository airportRepository;

    public void bootstrapData(Loader loader) {
        hotelRepository.setHotels(
                loader.loadHotels()
        );
        flightRepository.setFlights(
                loader.loadFlights()
        );
        carRepository.setProviders(
                loader.loadCars()
        );
        airportRepository.setAirports(
                loader.loadAirports()
        );
    }

    public void logBootstrapSummary() {
        log.info("Data successfully loaded: {} hotels, {} flights, {} cars, {} airports",
                hotelRepository.getHotels().size(),
                flightRepository.getFlights().size(),
                carRepository.getProviders().size(),
                airportRepository.getAirports().size()
        );
    }
}
