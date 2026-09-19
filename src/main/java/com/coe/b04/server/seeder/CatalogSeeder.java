package com.coe.b04.server.seeder;

import com.coe.b04.server.loader.Loader;
import com.coe.b04.server.model.Airport;
import com.coe.b04.server.model.Car;
import com.coe.b04.server.model.CarProvider;
import com.coe.b04.server.model.Flight;
import com.coe.b04.server.model.Hotel;
import com.coe.b04.server.reader.Reader;
import com.coe.b04.server.repository.CatalogRepository;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.DependsOn;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/*
 * Phase 1: uebertraegt die Mock-Daten aus den JSON-Dateien (lokaler
 * data/-Ordner oder Supabase S3, je nach Profil) nach Supabase/PostgreSQL.
 *
 * Startup (seedIfEmpty, idempotent):
 *   - Katalogtabelle leer  -> JSON laden und einfuegen
 *   - Katalogtabelle voll  -> nichts tun (bestehende Daten werden NIE
 *     ueberschrieben; auch ein geaendertes hotels.json loest kein
 *     automatisches Re-Seeding aus)
 *   - DB nicht konfiguriert/unerreichbar -> App startet trotzdem
 *     (Katalog bleibt In-Memory, nur ein Warn-Log)
 *
 * Admin (reseed, ausschliesslich explizit ueber POST /admin/catalog/reset):
 *   - loescht NUR die Katalogtabellen (Reservationsdaten bleiben unberuehrt)
 *     und seedet anschliessend erneut aus den JSON-Dateien
 */
@Slf4j
@Component
@DependsOn("envConfig")
public class CatalogSeeder {

    private final CatalogRepository catalogRepository;
    private final Reader reader;
    private final TransactionTemplate transactionTemplate;
    private final String dbPassword;

    public CatalogSeeder(CatalogRepository catalogRepository,
                         Reader reader,
                         TransactionTemplate transactionTemplate,
                         @Value("${supabase.db.password:}") String dbPassword) {
        this.catalogRepository = catalogRepository;
        this.reader = reader;
        this.transactionTemplate = transactionTemplate;
        this.dbPassword = dbPassword;
    }

    @PostConstruct
    public SeedResult seedIfEmpty() {
        if (dbPassword.isBlank()) {
            log.info("Catalog-Seeding uebersprungen: keine Supabase-DB konfiguriert "
                    + "(supabase.db.password fehlt).");
            return null;
        }
        try {
            SeedResult result = transactionTemplate.execute(status -> seedIfEmptyInternal());
            if (result.anythingSeeded()) {
                log.info("Catalog-Seeding abgeschlossen: {}", result);
            } else {
                log.info("Catalog-Seeding uebersprungen: Katalogtabellen sind bereits befuellt.");
            }
            return result;
        } catch (DataAccessException e) {
            log.warn("Catalog-Seeding uebersprungen (DB nicht erreichbar oder Schema fehlt, "
                    + "siehe db/catalog.sql): {}", e.getMessage());
            return null;
        }
    }

    /**
     * Admin-Reset: loescht die Katalogdaten und seedet neu aus den JSON-Dateien.
     * Betrifft ausschliesslich Katalogtabellen - Reservationsdaten bleiben unberuehrt.
     */
    public SeedResult reseed() {
        return transactionTemplate.execute(status -> {
            catalogRepository.deleteAllCatalogData();
            return seedIfEmptyInternal();
        });
    }

    private SeedResult seedIfEmptyInternal() {
        Loader loader = new Loader(reader);

        int airports = 0;
        if (catalogRepository.countAirports() == 0) {
            List<Airport> data = loader.loadAirports();
            data.forEach(catalogRepository::insertAirport);
            airports = data.size();
        }

        int flights = 0;
        if (catalogRepository.countFlights() == 0) {
            List<Flight> data = loader.loadFlights();
            catalogRepository.batchInsertFlights(data);
            flights = data.size();
        }

        int hotels = 0;
        int rooms = 0;
        if (catalogRepository.countHotels() == 0) {
            for (Hotel hotel : loader.loadHotels()) {
                UUID hotelId = catalogRepository.insertHotel(hotel);
                for (var room : hotel.getRoomTypes()) {
                    catalogRepository.insertRoomType(hotelId, room);
                    rooms++;
                }
                hotels++;
            }
        }

        int providers = 0;
        int cars = 0;
        int locations = 0;
        int extras = 0;
        if (catalogRepository.countProviders() == 0) {
            List<CarProvider> data = loader.loadCars();
            // Locations sind in cars.json pro Auto dupliziert -> pro external_id einmal anlegen
            Map<String, UUID> locationIds = new LinkedHashMap<>();
            for (CarProvider provider : data) {
                UUID providerId = catalogRepository.insertProvider(provider);
                providers++;
                for (Car car : provider.getCars()) {
                    UUID pickupId = locationIds.computeIfAbsent(
                            car.getLocations().getPickupLocation().getLocationId(),
                            id -> catalogRepository.insertLocation(car.getLocations().getPickupLocation()));
                    UUID returnId = locationIds.computeIfAbsent(
                            car.getLocations().getReturnLocation().getLocationId(),
                            id -> catalogRepository.insertLocation(car.getLocations().getReturnLocation()));
                    UUID carId = catalogRepository.insertCar(providerId, pickupId, returnId, car);
                    cars++;
                    if (car.getAdditionalExtras() != null) {
                        for (var extra : car.getAdditionalExtras()) {
                            catalogRepository.insertExtra(carId, extra);
                            extras++;
                        }
                    }
                }
            }
            locations = locationIds.size();
        }

        return new SeedResult(airports, flights, hotels, rooms, providers, cars, locations, extras);
    }
}
