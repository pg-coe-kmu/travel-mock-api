package com.coe.b04.server.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/*
 * DataSource fuer die Reservation-Phase (Supabase/PostgreSQL).
 * Hikari verbindet lazy, damit die App ohne konfigurierte DB startet.
 */
@Configuration
public class DataSourceConfig {

    @Bean
    public DataSource dataSource(@Value("${SUPABASE_URL:}") String supabaseUrl,
                                 @Value("${supabase.db.password:}") String dbPassword) {
        // Ohne SUPABASE_URL: wohlgeformte Fallback-URL, die Verbindung
        // schlaegt dann erst beim ersten Reservation-Zugriff fehl.
        String projectRef = supabaseUrl.isBlank()
                ? "localhost"
                : supabaseUrl.replace("https://", "").split("\\.")[0];

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
