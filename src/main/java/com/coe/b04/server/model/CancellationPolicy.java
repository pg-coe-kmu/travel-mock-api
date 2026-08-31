package com.coe.b04.server.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CancellationPolicy {
    private boolean isFreeCancellation;
    private CancellationDeadline cancellationDeadline;
}
