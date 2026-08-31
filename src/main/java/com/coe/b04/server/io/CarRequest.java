package com.coe.b04.server.io;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.List;

@Builder
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CarRequest {
    @NotBlank(message = "Pickup location is required")
    private String location;

    private String returnLocation;

    private String providerName;

    @Min(value = 0, message = "Minimum rating cannot be negative")
    private Double minRating;

    @Min(value = 0, message = "Driver age cannot be negative")
    private Integer driverAge;

    private String vehicleClass;

    private String categoryCode;

    private String brand;

    private String model;

    private String transmission;

    private String fuelType;

    private String driveType;

    private Boolean airCondition;

    @Builder.Default
    @Min(value = 0, message = "Number of seats cannot be negative")
    private Integer seats = 0;

    @Builder.Default
    @Min(value = 0, message = "Number of doors cannot be negative")
    private Integer doors = 0;

    @Builder.Default
    @Min(value = 0, message = "Number of large bags cannot be negative")
    private Integer largeBags = 0;

    @Builder.Default
    @Min(value = 0, message = "Number of small bags cannot be negative")
    private Integer smallBags = 0;

    private Double minPrice;

    private Double maxPrice;

    private Boolean freeCancellation;

    private List<String> includedServices;

    private String baseCurrency;
}
