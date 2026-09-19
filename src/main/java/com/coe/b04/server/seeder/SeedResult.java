package com.coe.b04.server.seeder;

/*
 * Ergebnis eines Seed-/Reseed-Laufs: Anzahl der in die jeweilige
 * Katalogtabelle geschriebenen Datensaetze. Wird fuer Logs und als
 * Antwort des Admin-Endpoints verwendet.
 */
public record SeedResult(
        int airports,
        int flights,
        int hotels,
        int rooms,
        int providers,
        int cars,
        int locations,
        int extras) {

    public boolean anythingSeeded() {
        return airports + flights + hotels + rooms + providers + cars + locations + extras > 0;
    }
}
