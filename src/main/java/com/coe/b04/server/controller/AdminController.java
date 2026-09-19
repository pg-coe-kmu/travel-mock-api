package com.coe.b04.server.controller;

import com.coe.b04.server.seeder.CatalogSeeder;
import com.coe.b04.server.seeder.SeedResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/*
 * Admin-Endpoints fuer die Katalog-/Mock-Daten.
 *
 * SICHERHEITSHINWEIS: Das Projekt besitzt aktuell KEIN Auth-/Security-Konzept
 * (kein spring-security). Der destruktive Reset-Endpoint existiert deshalb
 * nur im local-Profil - die deployte remote-App exponiert ihn gar nicht.
 * Ein lokaler Start mit .env kann weiterhin die echte Supabase-DB erreichen
 * (Entwicklungsmaschine, bewusst akzeptiert). Sobald ein Security-Konzept
 * existiert, den Endpoint stattdessen ueber eine Admin-Rolle absichern.
 */
@Tag(name = "Admin", description = "Admin operations for catalog mock data")
@Profile("local")
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
