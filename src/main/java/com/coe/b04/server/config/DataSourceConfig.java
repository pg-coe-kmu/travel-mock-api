package com.coe.b04.server.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/*
 * DataSource fuer die Reservation-Phase (Supabase/PostgreSQL).
 *
 * Die Verbindung wird aus den Supabase-Project-Credentials abgeleitet:
 *   - Host:   db.<project-ref>.supabase.co  (aus SUPABASE_URL)
 *   - User:   postgres (immer)
 *   - Passwort: SUPABASE_DB_PASSWORD (Dashboard -> Project Settings ->
 *              Database -> Connection string / Reset database password)
 *
 * Die Supabase API-Keys (publishable/secret) sind KEINE DB-Credentials.
 *
 * Hikari verbindet bewusst lazy (minimumIdle=0, initializationFailTimeout=-1),
 * damit die App ohne konfigurierte DB startet und die uebrigen Mock-Endpoints
 * lauffaehig bleiben. Erst der erste Reservation-Zugriff schlaegt dann fehl.
 */
@Configuration
public class DataSourceConfig {

    @Bean
    public DataSource dataSource(@Value("${SUPABASE_URL:}") String supabaseUrl,
                                 @Value("${supabase.db.password:}") String dbPassword) {
        String projectRef = supabaseUrl.replace("https://", "").split("\\.")[0];

        HikariConfig hikari = new HikariConfig();
        hikari.setJdbcUrl("jdbc:postgresql://db." + projectRef + ".supabase.co:5432/postgres");
        hikari.setUsername("postgres");
        hikari.setPassword(dbPassword);
        hikari.setMaximumPoolSize(5);
        hikari.setMinimumIdle(0);
        hikari.setInitializationFailTimeout(-1);
        hikari.setConnectionTimeout(5000);
        return new HikariDataSource(hikari);
    }
}
