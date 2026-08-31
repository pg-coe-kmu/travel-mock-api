package com.coe.b04.server.model;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ModelDeserializationTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void deserializesHotelFromMockStructure() {
        String json = """
                {
                  "hotelId": "HOT-1001",
                  "name": "Hotel Barcelona Center",
                  "city": "Barcelona",
                  "country": "Spain",
                  "stars": 4,
                  "rating": { "score": 4.6, "reviewCount": 1280 },
                  "hotelAmenities": ["Free WiFi", "Spa"],
                  "checkInTime": "15:00",
                  "checkOutTime": "11:00",
                  "baseCurrency": "EUR",
                  "roomTypes": [
                    {
                      "roomId": "ROOM-101",
                      "roomType": "Standard Double",
                      "board": "Breakfast",
                      "pricePerNight": 136.00,
                      "availableRooms": 5,
                      "maxOccupancy": { "adults": 2, "children": 1 },
                      "bedType": "1 King Bed",
                      "roomSizeSqm": 25,
                      "cancellationPolicy": {
                        "isFreeCancellation": true,
                        "cancellationDeadline": { "type": "AFTER_RESERVATION", "value": 24, "unit": "HOURS" }
                      },
                      "roomAmenities": ["Air Conditioning", "TV"]
                    },
                    {
                      "roomId": "ROOM-105",
                      "roomType": "Family Suite",
                      "board": "Breakfast",
                      "pricePerNight": 220.00,
                      "availableRooms": 4,
                      "maxOccupancy": { "adults": 2, "children": 2 },
                      "bedType": "1 King Bed and 2 Single Beds",
                      "roomSizeSqm": 45,
                      "cancellationPolicy": { "isFreeCancellation": false },
                      "roomAmenities": ["TV"]
                    }
                  ]
                }
                """;

        Hotel hotel = objectMapper.readValue(json, Hotel.class);

        assertEquals("HOT-1001", hotel.getHotelId());
        assertEquals(4, hotel.getStars());
        assertEquals(0, BigDecimal.valueOf(4.6).compareTo(hotel.getRating().getScore()));
        assertEquals(1280, hotel.getRating().getReviewCount());
        assertEquals(List.of("Free WiFi", "Spa"), hotel.getHotelAmenities());
        assertEquals(2, hotel.getRoomTypes().size());

        RoomType first = hotel.getRoomTypes().getFirst();
        assertEquals("ROOM-101", first.getRoomId());
        assertEquals(2, first.getMaxOccupancy().getAdults());
        assertEquals(1, first.getMaxOccupancy().getChildren());
        assertTrue(first.getCancellationPolicy().isFreeCancellation());
        assertNotNull(first.getCancellationPolicy().getCancellationDeadline());
        assertEquals("AFTER_RESERVATION", first.getCancellationPolicy().getCancellationDeadline().getType());
        assertEquals(24, first.getCancellationPolicy().getCancellationDeadline().getValue());
        assertEquals("HOURS", first.getCancellationPolicy().getCancellationDeadline().getUnit());

        RoomType second = hotel.getRoomTypes().get(1);
        assertFalse(second.getCancellationPolicy().isFreeCancellation());
        assertNull(second.getCancellationPolicy().getCancellationDeadline());
    }

    @Test
    void deserializesCarProviderFromMockStructure() {
        String json = """
                {
                  "providerId": "PROV-SIXT-01",
                  "providerName": "Sixt",
                  "rating": { "score": 4.5, "reviewCount": 3120 },
                  "baseCurrency": "EUR",
                  "providerPolicies": {
                    "minDriverAge": 21,
                    "youngDriverFeePerDay": 12.50,
                    "depositAmount": 300.00,
                    "acceptedPaymentMethods": ["Credit Card"]
                  },
                  "cars": [
                    {
                      "carId": "CAR-1001",
                      "vehicleClass": "Compact",
                      "categoryCode": "CDMR",
                      "brand": "VW",
                      "model": "Golf",
                      "availableVehicles": 8,
                      "locations": {
                        "pickupLocation": {
                          "locationId": "LOC-BCN-AP",
                          "name": "Barcelona Airport (BCN)",
                          "city": "Barcelona",
                          "address": "08820 El Prat de Llobregat, Barcelona",
                          "openingHours": "07:00 - 23:30"
                        },
                        "returnLocation": {
                          "locationId": "LOC-BCN-AP",
                          "name": "Barcelona Airport (BCN)",
                          "city": "Barcelona",
                          "address": "08820 El Prat de Llobregat, Barcelona",
                          "openingHours": "07:00 - 23:30"
                        }
                      },
                      "specifications": {
                        "transmission": "Automatic",
                        "fuelType": "Petrol",
                        "doors": 5,
                        "seats": 5,
                        "luggageCapacity": { "largeBags": 2, "smallBags": 1 },
                        "airCondition": true,
                        "driveType": "FWD"
                      },
                      "pricing": { "pricePerDay": 48.00, "totalPrice": 240.00, "rentalDays": 5 },
                      "includedServices": ["Unlimited Mileage"],
                      "cancellationPolicy": {
                        "isFreeCancellation": true,
                        "cancellationDeadline": { "type": "BEFORE_PICKUP", "value": 48, "unit": "HOURS" }
                      },
                      "additionalExtras": [
                        { "extraId": "EXT-GPS", "name": "Navigation System", "pricePerDay": 8.50, "priceType": "PER_DAY" }
                      ]
                    }
                  ]
                }
                """;

        CarProvider provider = objectMapper.readValue(json, CarProvider.class);

        assertEquals("Sixt", provider.getProviderName());
        assertEquals(21, provider.getProviderPolicies().getMinDriverAge());
        assertEquals(List.of("Credit Card"), provider.getProviderPolicies().getAcceptedPaymentMethods());

        Car car = provider.getCars().getFirst();
        assertEquals("CAR-1001", car.getCarId());
        assertEquals("Compact", car.getVehicleClass());
        assertEquals(8, car.getAvailableVehicles());
        assertEquals("Barcelona", car.getLocations().getPickupLocation().getCity());
        assertEquals("Barcelona", car.getLocations().getReturnLocation().getCity());
        assertEquals(5, car.getSpecifications().getSeats());
        assertEquals(2, car.getSpecifications().getLuggageCapacity().getLargeBags());
        assertTrue(car.getSpecifications().isAirCondition());
        assertEquals(0, new BigDecimal("48.00").compareTo(car.getPricing().getPricePerDay()));
        assertTrue(car.getCancellationPolicy().isFreeCancellation());
        assertEquals("BEFORE_PICKUP", car.getCancellationPolicy().getCancellationDeadline().getType());
        assertEquals("Navigation System", car.getAdditionalExtras().getFirst().getName());
    }
}
