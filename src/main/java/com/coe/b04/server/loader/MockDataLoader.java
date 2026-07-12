package com.coe.b04.server.loader;

import com.coe.b04.server.model.Car;
import com.coe.b04.server.model.Flight;
import com.coe.b04.server.model.Hotel;
import com.coe.b04.server.repository.CarRepository;
import com.coe.b04.server.repository.FlightRepository;
import com.coe.b04.server.repository.HotelRepository;
import com.coe.b04.server.utils.Constants;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static com.coe.b04.server.utils.Constants.*;


@Slf4j
@Component
public class MockDataLoader {

    private final HotelRepository hotelRepository;
    private final FlightRepository flightRepository;
    private final CarRepository carRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public MockDataLoader(HotelRepository hotelRepository,  FlightRepository flightRepository, CarRepository carRepository) {
        this.hotelRepository = hotelRepository;
        this.flightRepository = flightRepository;
        this.carRepository = carRepository;
    }

    @PostConstruct
    public void init() {
        log.info("Loading mock data for hotels, flights, and cars...");

        hotelRepository.setHotels(loadMockData(HOTELS_PATH, Hotel[].class));
        flightRepository.setFlights(loadMockData(FLIGHTS_PATH, Flight[].class));
        carRepository.setCars(loadMockData(CARS_PATH, Car[].class));

        log.info("Mock data loaded: {} hotels, {} flights, {} cars",
                hotelRepository.getHotels().size(),
                flightRepository.getFlights().size(),
                carRepository.getCars().size());

    }

    private <T> List<T> loadMockData(Path path, Class<T[]> clazz) throws IllegalStateException {
        try (InputStream is = Files.newInputStream(path)) {
            return Arrays.asList(objectMapper.readValue(is, clazz));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load mock data from " + path, e);
        }
    }
}
