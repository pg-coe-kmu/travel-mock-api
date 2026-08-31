package com.coe.b04.server.controller;

import com.coe.b04.server.io.CarRequest;
import com.coe.b04.server.io.CarResponse;
import com.coe.b04.server.model.Car;
import com.coe.b04.server.model.CarProvider;
import com.coe.b04.server.service.CarService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class CarControllerTest {

    private MockMvc mockMvc;
    private CarService carService;

    @BeforeEach
    void setUp() {
        carService = mock(CarService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new CarController(carService)).build();
    }

    @Test
    void searchCarsBindsRequestParams() throws Exception {
        when(carService.search(any())).thenReturn(new CarResponse(List.of()));

        mockMvc.perform(get("/api/cars/search")
                        .param("location", "Barcelona")
                        .param("providerName", "Sixt")
                        .param("vehicleClass", "Compact")
                        .param("minPrice", "40")
                        .param("maxPrice", "100")
                        .param("freeCancellation", "true")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        ArgumentCaptor<CarRequest> captor = ArgumentCaptor.forClass(CarRequest.class);
        verify(carService).search(captor.capture());
        CarRequest request = captor.getValue();
        assertEquals("Barcelona", request.getLocation());
        assertEquals("Sixt", request.getProviderName());
        assertEquals("Compact", request.getVehicleClass());
        assertEquals(40.0, request.getMinPrice());
        assertEquals(100.0, request.getMaxPrice());
        assertTrue(request.getFreeCancellation());
    }

    @Test
    void shouldReturnBadRequestWhenLocationMissing() throws Exception {
        mockMvc.perform(get("/api/cars/search")
                        .param("providerName", "Sixt"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(carService);
    }

    @Test
    void shouldUseDefaultsForOptionalParams() throws Exception {
        when(carService.search(any())).thenReturn(new CarResponse(List.of()));

        mockMvc.perform(get("/api/cars/search")
                        .param("location", "Barcelona"))
                .andExpect(status().isOk());

        ArgumentCaptor<CarRequest> captor = ArgumentCaptor.forClass(CarRequest.class);
        verify(carService).search(captor.capture());
        CarRequest request = captor.getValue();
        assertEquals(0, request.getSeats());
        assertEquals(0, request.getDoors());
        assertEquals(0, request.getLargeBags());
        assertEquals(0, request.getSmallBags());
        assertNull(request.getProviderName());
        assertNull(request.getMaxPrice());
    }

    @Test
    void bindsBooleanAndLists() throws Exception {
        when(carService.search(any())).thenReturn(new CarResponse(List.of()));

        mockMvc.perform(get("/api/cars/search")
                        .param("location", "Barcelona")
                        .param("airCondition", "true")
                        .param("includedServices", "Unlimited Mileage")
                        .param("includedServices", "Collision Damage Waiver (CDW)"))
                .andExpect(status().isOk());

        ArgumentCaptor<CarRequest> captor = ArgumentCaptor.forClass(CarRequest.class);
        verify(carService).search(captor.capture());
        CarRequest request = captor.getValue();
        assertTrue(request.getAirCondition());
        assertEquals(List.of("Unlimited Mileage", "Collision Damage Waiver (CDW)"),
                request.getIncludedServices());
    }

    @Test
    void shouldReturnBadRequestForNegativeSeats() throws Exception {
        mockMvc.perform(get("/api/cars/search")
                        .param("location", "Barcelona")
                        .param("seats", "-1"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(carService);
    }

    @Test
    void shouldReturnBadRequestForNegativeDriverAge() throws Exception {
        mockMvc.perform(get("/api/cars/search")
                        .param("location", "Barcelona")
                        .param("driverAge", "-1"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(carService);
    }

    @Test
    void returnsJsonResponseWithProviderData() throws Exception {
        CarProvider provider = CarProvider.builder()
                .providerId("PROV-SIXT")
                .providerName("Sixt")
                .cars(List.of(Car.builder().carId("CAR-1").build(), Car.builder().carId("CAR-2").build()))
                .build();
        when(carService.search(any())).thenReturn(new CarResponse(List.of(provider)));

        mockMvc.perform(get("/api/cars/search")
                        .param("location", "Barcelona")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.totalCount").value(2))
                .andExpect(jsonPath("$.providers[0].providerName").value("Sixt"))
                .andExpect(jsonPath("$.providers[0].cars[0].carId").value("CAR-1"));
    }
}
