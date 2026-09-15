package com.coe.b04.server.controller;

import com.coe.b04.server.io.CancelReservationRequest;
import com.coe.b04.server.io.CreateReservationRequest;
import com.coe.b04.server.io.ReservationResponse;
import com.coe.b04.server.service.ReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@Tag(name = "Reservations", description = "Reservation creation and status")
@RestController
@Validated
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
    @PostMapping("reservation/create")
    public ResponseEntity<ReservationResponse> create(@Valid @RequestBody CreateReservationRequest request) {
        ReservationResponse response = reservationService.create(request);
        return ResponseEntity
                .created(URI.create("/reservation/snapshot?reservationNumber=" + response.getReservationNumber()))
                .body(response);
    }

    @Operation(summary = "Get reservation",
            description = "Returns the stored reservation snapshot. Expired reservations "
                    + "are returned with status EXPIRED.")
    @ApiResponse(responseCode = "404", description = "Reservation not found")
    @GetMapping("reservation/snapshot")
    public ResponseEntity<ReservationResponse> get(
            @Parameter(description = "Reservation number", required = true)
            @RequestParam(value = "reservationNumber")
            @NotBlank(message = "reservationNumber is required") String reservationNumber) {
        return ResponseEntity.ok(reservationService.getByNumber(reservationNumber));
    }

    @Operation(summary = "Cancel reservation",
            description = "Cancels a PENDING reservation. Idempotent: cancelling an "
                    + "already cancelled reservation returns 409.")
    @ApiResponse(responseCode = "409", description = "Reservation is not PENDING")
    @PostMapping("reservation/cancel")
    public ResponseEntity<ReservationResponse> cancel(@Valid @RequestBody CancelReservationRequest request) {
        return ResponseEntity.ok(reservationService.cancel(request.getReservationNumber()));
    }
}
