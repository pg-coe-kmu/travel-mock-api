package com.coe.b04.server.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Ping-Endpoint zum Aufwecken des Servers: Render Free Tier legt
 * Web Services nach ~15 Minuten Inaktivitaet schlafen; ein externer
 * Pinger (Cron/Uptime-Service) ruft /ping auf, der naechste echte
 * Request startet dann nicht mehr kalt.
 */
@Tag(name = "System", description = "System utilities")
@RestController
public class SystemController {

    @Operation(summary = "Ping",
            description = "Alive check, wakes the sleeping server (Render free tier).")
    @GetMapping("ping")
    public ResponseEntity<String> ping() {
        return ResponseEntity.ok("pong");
    }
}
