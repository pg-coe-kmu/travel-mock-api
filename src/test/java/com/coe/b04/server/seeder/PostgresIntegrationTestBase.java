package com.coe.b04.server.seeder;

import com.coe.b04.server.TravelMockLocalApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

/*
 * Gemeinsame Basis fuer Integrationstests gegen ein echtes PostgreSQL
 * im Testcontainer: bootet den lokalen Spring-Context, verdrahtet die
 * bestehende DataSourceConfig via supabase.db.*-Properties auf den
 * Container und legt die DB-Schemata (db/reservations.sql, db/catalog.sql)
 * an. Guard gegen erneutes Anlegen bei wiederverwendetem Context.
 *
 * Der Container wird bewusst manuell (statischer Initialisierer) gestartet
 * und fuer die ganze JVM geteilt: Testcontainers 2.x teilt @Container-
 * Container NICHT ueber Testklassen hinweg; bei einem Container pro Klasse
 * wuerde der Spring-Context-Cache den alten Port behalten und gegen den
 * gestoppten Container laufen. Ryuk raeumt den Container beim JVM-Ende ab.
 */
@SpringBootTest(classes = TravelMockLocalApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("local")
abstract class PostgresIntegrationTestBase {

    static final PostgreSQLContainer postgres =
            new PostgreSQLContainer("postgres:16-alpine");

    static {
        // Im CI (GitHub Actions setzt CI=true) laufen die Container-Tests
        // nicht (siehe @DisabledIfEnvironmentVariable auf den Testklassen) -
        // der Container wird dort auch gar nicht erst gestartet.
        if (!"true".equals(System.getenv("CI"))) {
            postgres.start();
        }
    }

    @DynamicPropertySource
    static void dbProperties(DynamicPropertyRegistry registry) throws Exception {
        registry.add("supabase.db.host", postgres::getHost);
        registry.add("supabase.db.port", () -> postgres.getMappedPort(5432));
        registry.add("supabase.db.user", postgres::getUsername);
        registry.add("supabase.db.password", postgres::getPassword);
        applySchema("reservations", "db/reservations.sql");
        applySchema("hotels", "db/catalog.sql");
    }

    /*
     * Die App verbindet sich auf die Datenbank "postgres" (DataSourceConfig) -
     * das Schema muss dort liegen, nicht in der Container-Default-DB "test".
     */
    private static void applySchema(String guardTable, String schemaFile) throws Exception {
        String appJdbcUrl = "jdbc:postgresql://" + postgres.getHost() + ":"
                + postgres.getMappedPort(5432) + "/postgres";
        try (Connection connection = DriverManager.getConnection(
                appJdbcUrl, postgres.getUsername(), postgres.getPassword());
             Statement statement = connection.createStatement()) {
            try (ResultSet rs = statement.executeQuery(
                    "select to_regclass('public." + guardTable + "')")) {
                rs.next();
                if (rs.getString(1) != null) {
                    return;
                }
            }
            statement.execute(Files.readString(Path.of(schemaFile)));
        }
    }
}
