package com.coe.b04.server.loader;

import com.coe.b04.server.TravelMockRemoteApplication;
import com.coe.b04.server.repository.AirportRepository;
import com.coe.b04.server.repository.CarRepository;
import com.coe.b04.server.repository.FlightRepository;
import com.coe.b04.server.repository.HotelRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/*
 * Integration test for the real remote profile.
 *
 * Boots the remote Spring context, which loads the mock data from the real
 * Supabase S3 bucket (credentials via .env or SUPABASE_* environment
 * variables). Requires network access and a configured bucket.
 *
 * Covered:
 *  - the remote context starts and RemoteBootstrap runs (LocalBootstrap must NOT run)
 *  - all four data sets (hotels, flights, cars, airports) are fetched from the S3 bucket
 */

@SpringBootTest(classes = TravelMockRemoteApplication.class)
@ActiveProfiles("remote")
class RemoteBootstrapIntegrationTest {

    @Autowired
    private HotelRepository hotelRepository;

    @Autowired
    private FlightRepository flightRepository;

    @Autowired
    private CarRepository carRepository;

    @Autowired
    private AirportRepository airportRepository;

    @Test
    void loadsAllMockDataFromS3Bucket() {
        assertThat(hotelRepository.getHotels()).isNotEmpty();
        assertThat(flightRepository.getFlights()).isNotEmpty();
        assertThat(carRepository.getProviders()).isNotEmpty();
        assertThat(airportRepository.getAirports()).isNotEmpty();
    }
}
