package com.coe.b04.server.service;

import com.coe.b04.server.io.CarRequest;
import com.coe.b04.server.io.CarResponse;
import com.coe.b04.server.model.Car;
import com.coe.b04.server.model.CarProvider;
import com.coe.b04.server.repository.CarRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class CarServiceTest {

    private CarService carService;
    private CarRepository carRepository;

    @BeforeEach
    void setUp() {
        carRepository = mock(CarRepository.class);
        carService = new CarService(carRepository);
    }

    @Test
    void searchDelegatesToRepositoryAndWrapsResponse() {
        CarRequest request = CarRequest.builder().location("Barcelona").build();
        CarProvider provider1 = CarProvider.builder()
                .providerId("PROV-1")
                .cars(List.of(Car.builder().carId("CAR-1").build(), Car.builder().carId("CAR-2").build()))
                .build();
        CarProvider provider2 = CarProvider.builder()
                .providerId("PROV-2")
                .cars(List.of(Car.builder().carId("CAR-3").build()))
                .build();
        when(carRepository.findByLocationAndOptionals(request)).thenReturn(List.of(provider1, provider2));

        CarResponse response = carService.search(request);

        assertEquals(3, response.getTotalCount());
        assertEquals(2, response.getProviders().size());
        assertNotNull(response.getTimestamp());
        verify(carRepository).findByLocationAndOptionals(request);
    }

    @Test
    void searchHandlesNullResult() {
        CarRequest request = CarRequest.builder().location("Barcelona").build();
        when(carRepository.findByLocationAndOptionals(request)).thenReturn(null);

        CarResponse response = carService.search(request);

        assertEquals(0, response.getTotalCount());
        assertNull(response.getProviders());
    }

    @Test
    void getDetailsReturnsFilteredProvider() {
        CarProvider provider = CarProvider.builder()
                .providerId("PROV-1")
                .cars(List.of(Car.builder().carId("CAR-1").build()))
                .build();
        when(carRepository.findByProviderIdAndCarId("PROV-1", "CAR-1")).thenReturn(provider);

        CarProvider result = carService.getDetails("PROV-1", "CAR-1");

        assertEquals(provider, result);
        verify(carRepository).findByProviderIdAndCarId("PROV-1", "CAR-1");
    }

    @Test
    void getDetailsThrowsNotFound() {
        when(carRepository.findByProviderIdAndCarId("PROV-1", "CAR-UNKNOWN")).thenReturn(null);

        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> carService.getDetails("PROV-1", "CAR-UNKNOWN"));

        assertEquals(HttpStatus.NOT_FOUND, e.getStatusCode());
    }

    @Test
    void getDetailsReturnsFullProviderWhenCarIdNull() {
        CarProvider provider = CarProvider.builder().providerId("PROV-1").build();
        when(carRepository.findByProviderId("PROV-1")).thenReturn(provider);

        CarProvider result = carService.getDetails("PROV-1", null);

        assertEquals(provider, result);
        verify(carRepository).findByProviderId("PROV-1");
        verify(carRepository, never()).findByProviderIdAndCarId(anyString(), anyString());
    }

    @Test
    void getDetailsThrowsNotFoundForUnknownProvider() {
        when(carRepository.findByProviderId("PROV-UNKNOWN")).thenReturn(null);

        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> carService.getDetails("PROV-UNKNOWN", null));

        assertEquals(HttpStatus.NOT_FOUND, e.getStatusCode());
    }
}
