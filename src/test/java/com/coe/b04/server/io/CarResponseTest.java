package com.coe.b04.server.io;

import com.coe.b04.server.model.Car;
import com.coe.b04.server.model.CarProvider;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class CarResponseTest {

    @Test
    void totalCountSumsCarsAcrossProviders() {
        CarProvider provider1 = CarProvider.builder()
                .providerId("PROV-1")
                .cars(List.of(Car.builder().carId("CAR-1").build(), Car.builder().carId("CAR-2").build()))
                .build();
        CarProvider provider2 = CarProvider.builder()
                .providerId("PROV-2")
                .cars(List.of(Car.builder().carId("CAR-3").build()))
                .build();

        CarResponse response = new CarResponse(List.of(provider1, provider2));

        assertEquals(3, response.getTotalCount());
        assertEquals(2, response.getProviders().size());
        assertNotNull(response.getTimestamp());
    }

    @Test
    void totalCountIsZeroForNull() {
        CarResponse response = new CarResponse(null);

        assertEquals(0, response.getTotalCount());
    }
}
