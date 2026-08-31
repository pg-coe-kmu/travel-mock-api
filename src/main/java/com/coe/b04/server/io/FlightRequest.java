package com.coe.b04.server.io;

import com.coe.b04.server.enums.TravelClass;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Builder
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class FlightRequest {
    @NotBlank(message = "Origin is required")
    private String origin;

    private String originIataCode;

    @NotBlank(message = "Destination is required")
    private String destination;

    private String destinationIataCode;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate departureDate;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate departureDateFrom;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate departureDateTo;

    @Builder.Default
    @Min(value = 0, message = "Number of adults cannot be negative")
    private Integer numberOfAdults = 0;

    @Builder.Default
    @Min(value = 0, message = "Number of children cannot be negative")
    private Integer numberOfChildren = 0;

    @Builder.Default
    @Min(value = 0, message = "Number of infants cannot be negative")
    private Integer numberOfInfants = 0;

    private TravelClass travelClass;

    private Double maxPrice;
}
