package com.coe.b04.server.controller;

import com.coe.b04.server.io.CarRequest;
import com.coe.b04.server.io.CarResponse;
import com.coe.b04.server.service.CarService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Cars", description = "Rental car search")
@RestController
@RequestMapping("api/cars")
public class CarController {

    private final CarService carService;

    public CarController(CarService carService) {
        this.carService = carService;
    }

    @Operation(summary = "Search rental cars",
            description = "Searches rental cars by location, provider, vehicle specifications "
                    + "and optional filters such as price range and free cancellation.")
    @GetMapping("/search")
    public ResponseEntity<CarResponse> search(@Valid @ModelAttribute CarRequest carRequest) {
        CarResponse response = carService.search(carRequest);
        return ResponseEntity.ok(response);
    }
}
