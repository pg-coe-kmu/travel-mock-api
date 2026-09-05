package com.coe.b04.server.loader;

import com.coe.b04.server.enums.FileEntry;
import com.coe.b04.server.model.Airport;
import com.coe.b04.server.model.CarProvider;
import com.coe.b04.server.model.Flight;
import com.coe.b04.server.model.Hotel;
import com.coe.b04.server.reader.LocalReader;
import com.coe.b04.server.reader.S3Reader;

import java.util.List;

/*
 * Provides the mock data, either from the local data folder (LocalReader)
 * or from the Supabase S3 bucket (S3Reader). The bootstraps write the
 * provided data into the repositories.
 */
public class Loader {

    private final S3Reader s3Reader;
    private final LocalReader localReader;

    public Loader(S3Reader s3Reader) {
        this.s3Reader = s3Reader;
        this.localReader = null;
    }

    public Loader(LocalReader localReader) {
        this.s3Reader = null;
        this.localReader = localReader;
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
        return s3Reader != null
                ? s3Reader.load(fileName, clazz)
                : localReader.load(fileName, clazz);
    }
}
