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
public class HotelRequest {
    @NotBlank(message = "Destination is required")
    private String destination;

    private Integer stars;

    @Min(value = 0, message = "Minimum rating cannot be negative")
    private Double minRating;

    private List<String> hotelAmenities;

    private String roomType;

    private String board;

    private String bedType;

    private List<String> roomAmenities;

    @Builder.Default
    @Min(value = 0, message = "Number of adults cannot be negative")
    private Integer numberOfAdults = 0;

    @Builder.Default
    @Min(value = 0, message = "Number of children cannot be negative")
    private Integer numberOfChildren = 0;

    private Double minPrice;

    private Double maxPrice;

    private Boolean freeCancellation;
}
