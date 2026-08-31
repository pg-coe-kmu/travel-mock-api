package com.coe.b04.server.repository;

import com.coe.b04.server.io.CarRequest;
import com.coe.b04.server.model.Car;
import com.coe.b04.server.model.CarProvider;
import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Repository;

import java.util.List;

@Setter
@Getter
@Repository
public class CarRepository {

    private List<CarProvider> providers;

    /*
     * Finds providers by pickup location and optional filter parameters from the request:
     * providerName, minRating, driverAge, baseCurrency (provider level) and
     * returnLocation, vehicleClass, categoryCode, brand, model, transmission, fuelType, driveType,
     * airCondition, seats, doors, luggage, price range, freeCancellation, includedServices (car level).
     * The cars of each matched provider are reduced to the cars matching all given car filters.
     * Null/empty optional parameters are ignored.
     */
    public List<CarProvider> findByLocationAndOptionals(CarRequest request) {
        return providers.stream()
                .filter(provider -> request.getProviderName() == null
                        || provider.getProviderName().equalsIgnoreCase(request.getProviderName()))
                .filter(provider -> request.getMinRating() == null
                        || (provider.getRating() != null && provider.getRating().getScore().doubleValue() >= request.getMinRating()))
                .filter(provider -> request.getDriverAge() == null
                        || provider.getProviderPolicies().getMinDriverAge() <= request.getDriverAge())
                .filter(provider -> request.getBaseCurrency() == null
                        || provider.getBaseCurrency().equalsIgnoreCase(request.getBaseCurrency()))
                .map(provider -> withMatchingCars(provider, request))
                .filter(provider -> !provider.getCars().isEmpty())
                .toList();
    }

    /*
     * Returns a copy of the provider containing only the cars that match all given car filters.
     * The original provider (shared in-memory state) is left untouched.
     */
    private CarProvider withMatchingCars(CarProvider provider, CarRequest request) {
        List<Car> matchingCars = provider.getCars().stream()
                .filter(car -> matchesCarFilters(car, request))
                .toList();
        return provider.toBuilder().cars(matchingCars).build();
    }

    /*
     * Checks if a car matches all given car filters.
     */
    private boolean matchesCarFilters(Car car, CarRequest request) {
        return (request.getLocation() == null
                || car.getLocations().getPickupLocation().getCity().equalsIgnoreCase(request.getLocation()))
                && (request.getReturnLocation() == null
                || car.getLocations().getReturnLocation().getCity().equalsIgnoreCase(request.getReturnLocation()))
                && (request.getVehicleClass() == null || car.getVehicleClass().equalsIgnoreCase(request.getVehicleClass()))
                && (request.getCategoryCode() == null || car.getCategoryCode().equalsIgnoreCase(request.getCategoryCode()))
                && (request.getBrand() == null || car.getBrand().equalsIgnoreCase(request.getBrand()))
                && (request.getModel() == null || car.getModel().equalsIgnoreCase(request.getModel()))
                && (request.getTransmission() == null || car.getSpecifications().getTransmission().equalsIgnoreCase(request.getTransmission()))
                && (request.getFuelType() == null || car.getSpecifications().getFuelType().equalsIgnoreCase(request.getFuelType()))
                && (request.getDriveType() == null || car.getSpecifications().getDriveType().equalsIgnoreCase(request.getDriveType()))
                && (request.getAirCondition() == null || car.getSpecifications().isAirCondition() == request.getAirCondition())
                && car.getSpecifications().getSeats() >= request.getSeats()
                && car.getSpecifications().getDoors() >= request.getDoors()
                && car.getSpecifications().getLuggageCapacity().getLargeBags() >= request.getLargeBags()
                && car.getSpecifications().getLuggageCapacity().getSmallBags() >= request.getSmallBags()
                && (request.getMinPrice() == null || car.getPricing().getPricePerDay().doubleValue() >= request.getMinPrice())
                && (request.getMaxPrice() == null || car.getPricing().getPricePerDay().doubleValue() <= request.getMaxPrice())
                && (request.getFreeCancellation() == null
                || (car.getCancellationPolicy() != null
                && car.getCancellationPolicy().isFreeCancellation() == request.getFreeCancellation()))
                && hasAllServices(car.getIncludedServices(), request.getIncludedServices());
    }

    /*
     * Checks if the included services contain all requested services (case-insensitive).
     */
    private boolean hasAllServices(List<String> available, List<String> requested) {
        return requested == null || requested.isEmpty()
                || (available != null && requested.stream().allMatch(r ->
                available.stream().anyMatch(a -> a.equalsIgnoreCase(r))));
    }
}
