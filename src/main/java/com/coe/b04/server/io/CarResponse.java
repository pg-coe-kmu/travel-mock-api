package com.coe.b04.server.io;

import com.coe.b04.server.model.CarProvider;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class CarResponse {
    private LocalDateTime timestamp;
    private int totalCount;
    private List<CarProvider> providers;

    public CarResponse(List<CarProvider> providers) {
        this.timestamp = LocalDateTime.now();
        this.totalCount = providers != null ? providers.stream()
                .mapToInt(provider -> provider.getCars() != null ? provider.getCars().size() : 0)
                .sum() : 0;
        this.providers = providers;
    }
}
