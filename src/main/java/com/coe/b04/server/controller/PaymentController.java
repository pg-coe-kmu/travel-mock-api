package com.coe.b04.server.controller;

import com.coe.b04.server.io.PaymentRequest;
import com.coe.b04.server.io.PaymentResponse;
import com.coe.b04.server.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Payment", description = "Payment for reservations")
@RestController
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @Operation(summary = "Pay reservation",
            description = "Pays a PENDING reservation within its 30-minute validity. "
                    + "On success the reservation becomes a booking (bookingNumber). "
                    + "The held availability stays reserved.")
    @ApiResponse(responseCode = "402", description = "Payment failed (success=false)")
    @ApiResponse(responseCode = "409", description = "Reservation is expired, already paid or cancelled")
    @PostMapping("payment")
    public ResponseEntity<PaymentResponse> pay(@Valid @RequestBody PaymentRequest request) {
        return ResponseEntity.ok(paymentService.pay(request));
    }
}
