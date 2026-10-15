package com.coe.b04.server.controller;

import com.coe.b04.server.io.BookingResponse;
import com.coe.b04.server.io.CancelBookingRequest;
import com.coe.b04.server.service.BookingService;
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

@Tag(name = "Bookings", description = "Booking snapshot and management")
@RestController
@Validated
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @Operation(summary = "Get booking snapshot",
            description = "Returns the booking header plus the embedded reservation snapshot.")
    @ApiResponse(responseCode = "404", description = "Booking not found")
    @GetMapping("booking/snapshot")
    public ResponseEntity<BookingResponse> get(
            @Parameter(description = "Booking number", required = true)
            @RequestParam(value = "bookingNumber")
            @NotBlank(message = "bookingNumber is required") String bookingNumber) {
        return ResponseEntity.ok(bookingService.getByBookingNumber(bookingNumber));
    }

    @Operation(summary = "Cancel booking",
            description = "Cancels a booking if every policy-bearing item (rooms, cars) "
                    + "allows free cancellation and its deadline has not passed. "
                    + "Flights have no policy and never block. Held availability is released exactly once.")
    @ApiResponse(responseCode = "409", description = "Booking not cancellable (policy, already cancelled, or modified concurrently)")
    @PostMapping("booking/cancel")
    public ResponseEntity<BookingResponse> cancel(@Valid @RequestBody CancelBookingRequest request) {
        return ResponseEntity.ok(bookingService.cancel(request.getBookingNumber()));
    }
}
