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
                                 @Value("${supabase.db.host:localhost}") String dbHost,
                                 @Value("${supabase.db.port:5432}") String dbPort,
                                 @Value("${supabase.db.user:}") String dbUser,
                                 @Value("${supabase.db.password:}") String dbPassword) {
        String projectRef = supabaseUrl.isBlank()
                ? null
                : supabaseUrl.replace("https://", "").split("\\.")[0];

        // Supavisor-Pooler verlangt postgres.<project-ref>. Teil-Konfiguration
        // (nur Host/Passwort ohne SUPABASE_URL/User) schlaegt sonst erst spaet
        // mit kryptischem Auth-Fehler fehl - hier: klarer Startup-Fehler.
        if (projectRef == null && dbUser.isBlank() && !dbPassword.isBlank()) {
            throw new IllegalStateException(
                    "SUPABASE_URL (oder SUPABASE_DB_USER) fehlt: fuer den Pooler wird ein "
                            + "postgres.<project-ref>-Username benoetigt");
        }
        String username = !dbUser.isBlank()
                ? dbUser
                : (projectRef == null ? "postgres" : "postgres." + projectRef);

        HikariConfig hikari = new HikariConfig();
        hikari.setJdbcUrl("jdbc:postgresql://" + dbHost + ":" + dbPort + "/postgres");
        hikari.setUsername(username);
        hikari.setPassword(dbPassword);
        hikari.setMaximumPoolSize(5);
        hikari.setMinimumIdle(0);
        hikari.setInitializationFailTimeout(-1);
        // Grosszuegiger fuer Kaltstarts/hostende Netze (Render): 5s waren
        // fuer den ersten Connect zum Pooler zu knapp bemessen.
        hikari.setConnectionTimeout(15000);
        return new HikariDataSource(hikari);
    }
}
