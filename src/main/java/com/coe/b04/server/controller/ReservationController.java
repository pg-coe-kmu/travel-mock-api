package com.coe.b04.server.controller;

import com.coe.b04.server.io.CreateReservationRequest;
import com.coe.b04.server.io.ReservationDetailsResponse;
import com.coe.b04.server.io.ReservationResponse;
import com.coe.b04.server.service.ReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@Tag(name = "Reservations", description = "Reservation creation and status")
@RestController
public class ReservationController {

    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @Operation(summary = "Create reservation",
            description = "Creates a 30-minute reservation for a selected offer "
                    + "(hotel and/or flights and/or car). Prices are loaded from the "
                    + "current data and frozen at creation time.")
    @ApiResponse(responseCode = "400", description = "Invalid request or unknown ids")
    @PostMapping("reservations")
    public ResponseEntity<ReservationResponse> create(@Valid @RequestBody CreateReservationRequest request) {
        ReservationResponse response = reservationService.create(request);
        return ResponseEntity
                .created(URI.create("/reservations/" + response.getReservationNumber()))
                .body(response);
    }

    @Operation(summary = "Get reservation",
            description = "Returns the stored reservation snapshot. Expired reservations "
                    + "are returned with status EXPIRED.")
    @ApiResponse(responseCode = "404", description = "Reservation not found")
    @GetMapping("reservations/{reservationNumber}")
    public ResponseEntity<ReservationResponse> get(@PathVariable String reservationNumber) {
        return ResponseEntity.ok(reservationService.getByNumber(reservationNumber));
    }

    @Operation(summary = "Get reservation details",
            description = "Like GET, additionally resolves the full hotel/flight/car "
                    + "offer contents via the stored ids.")
    @ApiResponse(responseCode = "404", description = "Reservation not found")
    @GetMapping("reservations/{reservationNumber}/details")
    public ResponseEntity<ReservationDetailsResponse> details(@PathVariable String reservationNumber) {
        return ResponseEntity.ok(reservationService.getDetails(reservationNumber));
    }

    @Operation(summary = "Cancel reservation",
            description = "Cancels a PENDING reservation. Idempotent: cancelling an "
                    + "already cancelled reservation returns 409.")
    @ApiResponse(responseCode = "409", description = "Reservation is not PENDING")
    @PostMapping("reservations/{reservationNumber}/cancel")
    public ResponseEntity<ReservationResponse> cancel(@PathVariable String reservationNumber) {
        return ResponseEntity.ok(reservationService.cancel(reservationNumber));
    }
}
