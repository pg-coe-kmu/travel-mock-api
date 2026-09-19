package com.coe.b04.server.controller;

import com.coe.b04.server.seeder.CatalogSeeder;
import com.coe.b04.server.seeder.SeedResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/*
 * Admin-Endpoints fuer die Katalog-/Mock-Daten.
 *
 * SICHERHEITSHINWEIS (Phase 1): Das Projekt besitzt aktuell KEIN
 * Auth-/Security-Konzept (kein spring-security). Der Reset-Endpoint ist
 * daher bewusst als eigene Admin-Stelle vorbereitet, aber ungeschuetzt.
 * Sobald ein Security-Konzept existiert, muss dieser Endpoint als erstes
 * abgesichert werden (Admin-Rolle).
 */
@Tag(name = "Admin", description = "Admin operations for catalog mock data")
@RestController
public class AdminController {

    private final CatalogSeeder catalogSeeder;

    public AdminController(CatalogSeeder catalogSeeder) {
        this.catalogSeeder = catalogSeeder;
    }

    @Operation(summary = "Reset and reseed catalog data",
            description = "Deletes all catalog data (hotels, rooms, flights, cars, providers, "
                    + "locations, airports) and reseeds it from the JSON mock data files. "
                    + "Reservation data is NOT affected.")
    @PostMapping("/admin/catalog/reset")
    public ResponseEntity<SeedResult> resetCatalog() {
        return ResponseEntity.ok(catalogSeeder.reseed());
    }
}
