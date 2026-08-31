package com.coe.b04.server.repository;

import com.coe.b04.server.io.CarRequest;
import com.coe.b04.server.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CarRepositoryTest {

    private CarRepository repository;
    private CarProvider sixt;
    private CarProvider hertz;

    @BeforeEach
    void setUp() {
        repository = new CarRepository();

        sixt = CarProvider.builder()
                .providerId("PROV-SIXT")
                .providerName("Sixt")
                .rating(new Rating(BigDecimal.valueOf(4.5), 3000))
                .baseCurrency("EUR")
                .providerPolicies(new ProviderPolicies(21, new BigDecimal("12.50"), new BigDecimal("300.00"), List.of("Credit Card")))
                .cars(List.of(
                        car("CAR-1", "Compact", "Barcelona", 5, 5, 2, 1, "48.00", true,
                                List.of("Unlimited Mileage", "Collision Damage Waiver (CDW)")),
                        car("CAR-2", "SUV", "Madrid", 5, 5, 3, 2, "82.00", false,
                                List.of("Unlimited Mileage"))))
                .build();

        hertz = CarProvider.builder()
                .providerId("PROV-HERTZ")
                .providerName("Hertz")
                .rating(new Rating(BigDecimal.valueOf(4.2), 1800))
                .baseCurrency("EUR")
                .providerPolicies(new ProviderPolicies(23, new BigDecimal("10.00"), new BigDecimal("200.00"), List.of("Credit Card")))
                .cars(List.of(
                        car("CAR-3", "Van", "Barcelona", 7, 5, 4, 2, "92.00", true,
                                List.of("Unlimited Mileage"))))
                .build();

        repository.setProviders(List.of(sixt, hertz));
    }

    private Car car(String carId, String vehicleClass, String city, int seats, int doors,
                    int largeBags, int smallBags, String pricePerDay, boolean freeCancellation,
                    List<String> includedServices) {
        return Car.builder()
                .carId(carId)
                .vehicleClass(vehicleClass)
                .brand("VW")
                .model("Golf")
                .locations(new CarLocations(
                        new CarLocation("LOC-1", city + " Airport", city, "Address", "07:00 - 23:00"),
                        new CarLocation("LOC-1", city + " Airport", city, "Address", "07:00 - 23:00")))
                .specifications(new CarSpecifications("Automatic", "Petrol", doors, seats,
                        new LuggageCapacity(largeBags, smallBags), true, "FWD"))
                .pricing(new CarPricing(new BigDecimal(pricePerDay),
                        new BigDecimal(pricePerDay).multiply(BigDecimal.valueOf(5)), 5))
                .includedServices(includedServices)
                .cancellationPolicy(new CancellationPolicy(freeCancellation, null))
                .build();
    }

    @Test
    void filtersByLocationCity() {
        List<CarProvider> result = repository.findByLocationAndOptionals(
                CarRequest.builder().location("Barcelona").build());

        assertEquals(2, result.size());
        assertEquals(List.of("CAR-1"), result.getFirst().getCars().stream().map(Car::getCarId).toList());
        assertEquals(List.of("CAR-3"), result.get(1).getCars().stream().map(Car::getCarId).toList());
    }

    @Test
    void reducesCarsAndExcludesEmptyProviders() {
        List<CarProvider> result = repository.findByLocationAndOptionals(
                CarRequest.builder().location("Barcelona").maxPrice(50.0).build());

        assertEquals(1, result.size());
        assertEquals("Sixt", result.getFirst().getProviderName());
        assertEquals(List.of("CAR-1"), result.getFirst().getCars().stream().map(Car::getCarId).toList());
    }

    @Test
    void filtersByProviderName() {
        List<CarProvider> result = repository.findByLocationAndOptionals(
                CarRequest.builder().location("Barcelona").providerName("hertz").build());

        assertEquals(1, result.size());
        assertEquals("Hertz", result.getFirst().getProviderName());
    }

    @Test
    void filtersByDriverAge() {
        List<CarProvider> result = repository.findByLocationAndOptionals(
                CarRequest.builder().location("Barcelona").driverAge(22).build());

        assertEquals(1, result.size());
        assertEquals("Sixt", result.getFirst().getProviderName());
    }

    @Test
    void filtersByMinRating() {
        List<CarProvider> result = repository.findByLocationAndOptionals(
                CarRequest.builder().location("Barcelona").minRating(4.4).build());

        assertEquals(1, result.size());
        assertEquals("Sixt", result.getFirst().getProviderName());
    }

    @Test
    void filtersBySeatsMinimum() {
        List<CarProvider> result = repository.findByLocationAndOptionals(
                CarRequest.builder().location("Barcelona").seats(7).build());

        assertEquals(1, result.size());
        assertEquals("Hertz", result.getFirst().getProviderName());
        assertEquals(List.of("CAR-3"), result.getFirst().getCars().stream().map(Car::getCarId).toList());
    }

    @Test
    void filtersByFreeCancellation() {
        List<CarProvider> result = repository.findByLocationAndOptionals(
                CarRequest.builder().location("Madrid").freeCancellation(false).build());

        assertEquals(1, result.size());
        assertEquals("Sixt", result.getFirst().getProviderName());
        assertEquals(List.of("CAR-2"), result.getFirst().getCars().stream().map(Car::getCarId).toList());
    }

    @Test
    void filtersByIncludedServicesAllRequired() {
        List<CarProvider> result = repository.findByLocationAndOptionals(
                CarRequest.builder().location("Barcelona")
                        .includedServices(List.of("Unlimited Mileage", "Collision Damage Waiver (CDW)"))
                        .build());

        assertEquals(1, result.size());
        assertEquals(List.of("CAR-1"), result.getFirst().getCars().stream().map(Car::getCarId).toList());
    }

    @Test
    void filtersByBaseCurrency() {
        List<CarProvider> result = repository.findByLocationAndOptionals(
                CarRequest.builder().location("Barcelona").baseCurrency("CHF").build());

        assertTrue(result.isEmpty());
    }

    @Test
    void doesNotMutateSharedData() {
        repository.findByLocationAndOptionals(
                CarRequest.builder().location("Barcelona").maxPrice(50.0).build());

        assertEquals(2, sixt.getCars().size());
        assertEquals(2, repository.getProviders().getFirst().getCars().size());
    }
}
