package com.smartlogistics.database.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.stream.Collectors;

/**
 * Exécute schema.sql de manière idempotente au démarrage du nœud central.
 */
public final class SchemaInitializer {
    private static final Logger log = LoggerFactory.getLogger(SchemaInitializer.class);

    private SchemaInitializer() {}

    public static void initialize() throws SQLException, IOException {
        String sqlScript;
        try (InputStream is = SchemaInitializer.class.getClassLoader().getResourceAsStream("schema.sql")) {
            if (is == null) {
                throw new IOException("Fichier schema.sql introuvable sur le classpath.");
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                sqlScript = reader.lines()
                        .filter(line -> !line.trim().startsWith("--"))
                        .collect(Collectors.joining("\n"));
            }
        }

        String[] statements = sqlScript.split(";");
        try (Connection conn = DataSourceFactory.getConnection();
             Statement stmt = conn.createStatement()) {
            for (String raw : statements) {
                String sql = raw.trim();
                if (!sql.isEmpty()) {
                    stmt.execute(sql);
                }
            }
            log.info("Schéma de base de données Smart Logistics vérifié / initialisé avec succès.");
        }
    }
}
