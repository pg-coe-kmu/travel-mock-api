package com.coe.b04.server.controller;

import com.coe.b04.server.enums.TravelClass;
import com.coe.b04.server.io.CarRequest;
import com.coe.b04.server.io.CarResponse;
import com.coe.b04.server.io.FlightRequest;
import com.coe.b04.server.io.FlightResponse;
import com.coe.b04.server.io.HotelRequest;
import com.coe.b04.server.io.HotelResponse;
import com.coe.b04.server.model.Car;
import com.coe.b04.server.model.CarProvider;
import com.coe.b04.server.service.CarService;
import com.coe.b04.server.service.FlightService;
import com.coe.b04.server.service.HotelService;
import com.coe.b04.server.utils.TravelClassConverter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class SearchControllerTest {

    private MockMvc mockMvc;
    private CarService carService;
    private HotelService hotelService;
    private FlightService flightService;

    @BeforeEach
    void setUp() {
        carService = mock(CarService.class);
        hotelService = mock(HotelService.class);
        flightService = mock(FlightService.class);

        DefaultFormattingConversionService conversionService = new DefaultFormattingConversionService();
        conversionService.addConverter(new TravelClassConverter());

        mockMvc = MockMvcBuilders.standaloneSetup(new SearchController(carService, hotelService, flightService))
                .setConversionService(conversionService)
                .build();
    }

    @Test
    void searchCarsBindsRequestParams() throws Exception {
        when(carService.search(any())).thenReturn(new CarResponse(List.of()));

        mockMvc.perform(get("/search/cars")
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
    void carSearchShouldReturnBadRequestWhenLocationMissing() throws Exception {
        mockMvc.perform(get("/search/cars")
                        .param("providerName", "Sixt"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(carService);
    }

    @Test
    void carSearchShouldUseDefaultsForOptionalParams() throws Exception {
        when(carService.search(any())).thenReturn(new CarResponse(List.of()));

        mockMvc.perform(get("/search/cars")
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
    void carSearchBindsBooleanAndLists() throws Exception {
        when(carService.search(any())).thenReturn(new CarResponse(List.of()));

        mockMvc.perform(get("/search/cars")
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
    void carSearchShouldReturnBadRequestForNegativeSeats() throws Exception {
        mockMvc.perform(get("/search/cars")
                        .param("location", "Barcelona")
                        .param("seats", "-1"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(carService);
    }

    @Test
    void carSearchShouldReturnBadRequestForNegativeDriverAge() throws Exception {
        mockMvc.perform(get("/search/cars")
                        .param("location", "Barcelona")
                        .param("driverAge", "-1"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(carService);
    }

    @Test
    void carSearchReturnsJsonResponseWithProviderData() throws Exception {
        CarProvider provider = CarProvider.builder()
                .providerId("PROV-SIXT")
                .providerName("Sixt")
                .cars(List.of(Car.builder().carId("CAR-1").build(), Car.builder().carId("CAR-2").build()))
                .build();
        when(carService.search(any())).thenReturn(new CarResponse(List.of(provider)));

        mockMvc.perform(get("/search/cars")
                        .param("location", "Barcelona")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.totalCount").value(2))
                .andExpect(jsonPath("$.providers[0].providerName").value("Sixt"))
                .andExpect(jsonPath("$.providers[0].cars[0].carId").value("CAR-1"));
    }

    @Test
    void searchHotelsBindsRequestParams() throws Exception {
        when(hotelService.search(any())).thenReturn(new HotelResponse(List.of()));

        mockMvc.perform(get("/search/hotels")
                        .param("destination", "Barcelona")
                        .param("stars", "4")
                        .param("roomType", "Standard Double")
                        .param("maxPrice", "150")
                        .param("board", "Breakfast")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        ArgumentCaptor<HotelRequest> captor = ArgumentCaptor.forClass(HotelRequest.class);
        verify(hotelService).search(captor.capture());
        HotelRequest request = captor.getValue();
        assertEquals("Barcelona", request.getDestination());
        assertEquals(4, request.getStars());
        assertEquals("Standard Double", request.getRoomType());
        assertEquals(150.0, request.getMaxPrice());
        assertEquals("Breakfast", request.getBoard());
    }

    @Test
    void hotelSearchShouldReturnBadRequestWhenDestinationMissing() throws Exception {
        mockMvc.perform(get("/search/hotels")
                        .param("stars", "4"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(hotelService);
    }

    @Test
    void hotelSearchShouldUseDefaultsForOptionalParams() throws Exception {
        when(hotelService.search(any())).thenReturn(new HotelResponse(List.of()));

        mockMvc.perform(get("/search/hotels")
                        .param("destination", "Barcelona"))
                .andExpect(status().isOk());

        ArgumentCaptor<HotelRequest> captor = ArgumentCaptor.forClass(HotelRequest.class);
        verify(hotelService).search(captor.capture());
        HotelRequest request = captor.getValue();
        assertEquals(0, request.getNumberOfAdults());
        assertEquals(0, request.getNumberOfChildren());
        assertNull(request.getStars());
        assertNull(request.getRoomType());
        assertNull(request.getMaxPrice());
    }

    @Test
    void hotelSearchBindsBooleanAndPriceFilters() throws Exception {
        when(hotelService.search(any())).thenReturn(new HotelResponse(List.of()));

        mockMvc.perform(get("/search/hotels")
                        .param("destination", "Barcelona")
                        .param("freeCancellation", "true")
                        .param("minPrice", "50")
                        .param("bedType", "1 King Bed"))
                .andExpect(status().isOk());

        ArgumentCaptor<HotelRequest> captor = ArgumentCaptor.forClass(HotelRequest.class);
        verify(hotelService).search(captor.capture());
        HotelRequest request = captor.getValue();
        assertTrue(request.getFreeCancellation());
        assertEquals(50.0, request.getMinPrice());
        assertEquals("1 King Bed", request.getBedType());
    }

    @Test
    void hotelSearchBindsAmenitiesAsList() throws Exception {
        when(hotelService.search(any())).thenReturn(new HotelResponse(List.of()));

        mockMvc.perform(get("/search/hotels")
                        .param("destination", "Barcelona")
                        .param("hotelAmenities", "Free WiFi")
                        .param("hotelAmenities", "Spa"))
                .andExpect(status().isOk());

        ArgumentCaptor<HotelRequest> captor = ArgumentCaptor.forClass(HotelRequest.class);
        verify(hotelService).search(captor.capture());
        assertEquals(List.of("Free WiFi", "Spa"), captor.getValue().getHotelAmenities());
    }

    @Test
    void hotelSearchShouldReturnBadRequestForNegativeMinRating() throws Exception {
        mockMvc.perform(get("/search/hotels")
                        .param("destination", "Barcelona")
                        .param("minRating", "-1"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(hotelService);
    }

    @Test
    void hotelSearchShouldReturnBadRequestForNegativeGuests() throws Exception {
        mockMvc.perform(get("/search/hotels")
                        .param("destination", "Barcelona")
                        .param("numberOfAdults", "-1"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(hotelService);
    }

    @Test
    void hotelSearchReturnsJsonResponseWithCount() throws Exception {
        when(hotelService.search(any())).thenReturn(new HotelResponse(List.of()));

        mockMvc.perform(get("/search/hotels")
                        .param("destination", "Barcelona")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.totalCount").value(0))
                .andExpect(jsonPath("$.hotels").isEmpty());
    }

    @Test
    void searchFlightsBindsTravelClassFromDisplayValue() throws Exception {
        when(flightService.search(any())).thenReturn(new FlightResponse(List.of()));

        mockMvc.perform(get("/search/flights")
                        .param("origin", "Paris")
                        .param("destination", "Rome")
                        .param("departureDate", "2026-08-01")
                        .param("numberOfAdults", "1")
                        .param("travelClass", "Business")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        ArgumentCaptor<FlightRequest> requestCaptor = ArgumentCaptor.forClass(FlightRequest.class);
        verify(flightService).search(requestCaptor.capture());
        assertEquals(TravelClass.BUSINESS, requestCaptor.getValue().getTravelClass());
    }

    @Test
    void flightSearchShouldBindTravelClassFromEnumName() throws Exception {
        when(flightService.search(any())).thenReturn(new FlightResponse(List.of()));

        mockMvc.perform(get("/search/flights")
                        .param("origin", "Paris")
                        .param("destination", "Rome")
                        .param("departureDate", "2026-08-01")
                        .param("numberOfAdults", "1")
                        .param("travelClass", "BUSINESS"))
                .andExpect(status().isOk());

        ArgumentCaptor<FlightRequest> captor = ArgumentCaptor.forClass(FlightRequest.class);
        verify(flightService).search(captor.capture());

        assertEquals(TravelClass.BUSINESS, captor.getValue().getTravelClass());
    }

    @Test
    void flightSearchShouldReturnBadRequestForInvalidTravelClass() throws Exception {
        mockMvc.perform(get("/search/flights")
                        .param("origin", "Paris")
                        .param("destination", "Rome")
                        .param("departureDate", "2026-08-01")
                        .param("numberOfAdults", "1")
                        .param("travelClass", "INVALID"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(flightService);
    }

    @Test
    void flightSearchShouldAllowMissingTravelClass() throws Exception {
        when(flightService.search(any())).thenReturn(new FlightResponse(List.of()));

        mockMvc.perform(get("/search/flights")
                        .param("origin", "Paris")
                        .param("destination", "Rome")
                        .param("departureDate", "2026-08-01")
                        .param("numberOfAdults", "1"))
                .andExpect(status().isOk());

        ArgumentCaptor<FlightRequest> captor = ArgumentCaptor.forClass(FlightRequest.class);
        verify(flightService).search(captor.capture());

        assertNull(captor.getValue().getTravelClass());
    }

    @Test
    void flightSearchShouldReturnBadRequestWhenOriginMissing() throws Exception {
        mockMvc.perform(get("/search/flights")
                        .param("destination", "Rome")
                        .param("departureDate", "2026-08-01")
                        .param("numberOfAdults", "1"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(flightService);
    }

    @Test
    void flightSearchShouldReturnBadRequestForNegativePassengerCounts() throws Exception {
        mockMvc.perform(get("/search/flights")
                        .param("origin", "Paris")
                        .param("destination", "Rome")
                        .param("departureDate", "2026-08-01")
                        .param("numberOfAdults", "-1"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(flightService);
    }

    @Test
    void flightSearchBindsDatesAndMaxPrice() throws Exception {
        when(flightService.search(any())).thenReturn(new FlightResponse(List.of()));

        mockMvc.perform(get("/search/flights")
                        .param("origin", "Paris")
                        .param("destination", "Rome")
                        .param("departureDate", "2026-08-01")
                        .param("numberOfAdults", "2")
                        .param("numberOfChildren", "1")
                        .param("maxPrice", "500"))
                .andExpect(status().isOk());

        ArgumentCaptor<FlightRequest> captor = ArgumentCaptor.forClass(FlightRequest.class);
        verify(flightService).search(captor.capture());
        FlightRequest request = captor.getValue();
        assertEquals(LocalDate.of(2026, 8, 1), request.getDepartureDate());
        assertEquals(2, request.getNumberOfAdults());
        assertEquals(1, request.getNumberOfChildren());
        assertEquals(0, request.getNumberOfInfants());
        assertEquals(500.0, request.getMaxPrice());
    }

    @Test
    void flightSearchShouldReturnBadRequestForInvalidDate() throws Exception {
        mockMvc.perform(get("/search/flights")
                        .param("origin", "Paris")
                        .param("destination", "Rome")
                        .param("departureDate", "not-a-date")
                        .param("numberOfAdults", "1"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(flightService);
    }

    @Test
    void flightSearchReturnsJsonResponseWithCount() throws Exception {
        when(flightService.search(any())).thenReturn(new FlightResponse(List.of()));

        mockMvc.perform(get("/search/flights")
                        .param("origin", "Paris")
                        .param("destination", "Rome")
                        .param("departureDate", "2026-08-01")
                        .param("numberOfAdults", "1")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.totalCount").value(0))
                .andExpect(jsonPath("$.flights").isEmpty());
    }
}
