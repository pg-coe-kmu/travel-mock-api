package com.coe.b04.server.loader;

import com.coe.b04.server.enums.FileEntry;
import com.coe.b04.server.model.Airport;
import com.coe.b04.server.model.CarProvider;
import com.coe.b04.server.model.Flight;
import com.coe.b04.server.model.Hotel;
import com.coe.b04.server.reader.Reader;

import java.util.List;

/*
 * Provides the mock data via the given Reader (local data folder or
 * Supabase S3 bucket). The bootstraps write the provided data into the
 * repositories.
 */
public class Loader {

    private final Reader reader;

    public Loader(Reader reader) {
        this.reader = reader;
    }

    public List<Hotel> loadHotels() {
        return load(FileEntry.HOTELS_FILE.getFilename(), Hotel[].class);
    }

    public List<Flight> loadFlights() {
        return load(FileEntry.FLIGHTS_FILE.getFilename(), Flight[].class);
    }

    public List<CarProvider> loadCars() {
        return load(FileEntry.CARS_FILE.getFilename(), CarProvider[].class);
    }

    public List<Airport> loadAirports() {
        return load(FileEntry.AIRPORTS_FILE.getFilename(), Airport[].class);
    }

    private <T> List<T> load(String fileName, Class<T[]> clazz) {
        return reader.load(fileName, clazz);
    }
}
