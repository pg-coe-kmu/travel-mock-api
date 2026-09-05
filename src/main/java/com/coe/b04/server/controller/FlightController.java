package com.coe.b04.server.controller;

import com.coe.b04.server.io.FlightRequest;
import com.coe.b04.server.io.FlightResponse;
import com.coe.b04.server.service.FlightService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Flights", description = "Flight search")
@RestController
@RequestMapping("api/flights")
public class FlightController {

    private final FlightService flightService;

    public FlightController(FlightService flightService) {
        this.flightService = flightService;
    }

    @Operation(summary = "Search flights",
            description = "Searches flights by origin, destination, travel dates, passenger counts "
                    + "and optional filters such as travel class and maximum price.")
    @GetMapping("/search")
    public ResponseEntity<FlightResponse> searchFlights(@Valid @ModelAttribute FlightRequest flightRequest) {
        FlightResponse response = flightService.search(flightRequest);
        return ResponseEntity.ok(response);
    }
}
