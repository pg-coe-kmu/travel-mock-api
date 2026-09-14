package com.coe.b04.server.controller;

import com.coe.b04.server.model.CarProvider;
import com.coe.b04.server.model.Flight;
import com.coe.b04.server.model.Hotel;
import com.coe.b04.server.service.CarService;
import com.coe.b04.server.service.FlightService;
import com.coe.b04.server.service.HotelService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Details", description = "Search specific car, hotel or flight by id")
@RestController
public class DetailsController {

    private final CarService carService;
    private final HotelService hotelService;
    private final FlightService flightService;

    public DetailsController(CarService carService, HotelService hotelService, FlightService flightService) {
        this.carService = carService;
        this.hotelService = hotelService;
        this.flightService = flightService;
    }

    @Operation(summary = "Get car details",
            description = "Returns the provider matching providerId with only the car matching carId, "
                    + "or the full provider when carId is omitted.")
    @ApiResponse(responseCode = "404", description = "Provider or car not found")
    @GetMapping("details/car")
    public ResponseEntity<CarProvider> carDetails(
            @Parameter(description = "Car provider id", required = true) @RequestParam String providerId,
            @Parameter(description = "Car id; when omitted the full provider is returned", required = false)
            @RequestParam(required = false) String carId) {
        return ResponseEntity.ok(carService.getDetails(providerId, carId));
    }

    @Operation(summary = "Get hotel details",
            description = "Returns the hotel matching hotelId with only the room matching roomId, "
                    + "or the full hotel when roomId is omitted.")
    @ApiResponse(responseCode = "404", description = "Hotel or room not found")
    @GetMapping("details/hotel")
    public ResponseEntity<Hotel> hotelDetails(
            @Parameter(description = "Hotel id", required = true) @RequestParam String hotelId,
            @Parameter(description = "Room id; when omitted the full hotel is returned", required = false)
            @RequestParam(required = false) String roomId) {
        return ResponseEntity.ok(hotelService.getDetails(hotelId, roomId));
    }

    @Operation(summary = "Get flight details",
            description = "Returns the flight matching flightId.")
    @ApiResponse(responseCode = "404", description = "Flight not found")
    @GetMapping("details/flight")
    public ResponseEntity<Flight> flightDetails(
            @Parameter(description = "Flight id", required = true) @RequestParam String flightId) {
        return ResponseEntity.ok(flightService.getDetails(flightId));
    }
}
