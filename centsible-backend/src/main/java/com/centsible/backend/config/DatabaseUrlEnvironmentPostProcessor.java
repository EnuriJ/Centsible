package com.centsible.backend.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * If DATABASE_URL is a {@code postgres://user:password@host/db} URL (the format most hosting
 * and database providers give you), fill in the datasource settings from it, so it can be pasted
 * in as-is. A {@code jdbc:} URL, or no DATABASE_URL at all (local development), changes nothing.
 * See {@link DatabaseUrl} for the conversion rules.
 *
 * Neon's Vercel integration provides two URLs: DATABASE_URL (through a connection pooler) and
 * DATABASE_URL_UNPOOLED (direct). The app keeps its own small connection pool and relies on
 * ordinary session behaviour, so when DATABASE_URL_UNPOOLED is present it is used instead.
 */
public class DatabaseUrlEnvironmentPostProcessor implements EnvironmentPostProcessor {

    static final String SOURCE_NAME = "centsibleDatabaseUrl";

    private static final String UNPOOLED_VARIABLE = "DATABASE_URL_UNPOOLED";
    private static final String URL_VARIABLE = "DATABASE_URL";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        Optional<DatabaseUrl.Parts> chosen = DatabaseUrl.parse(environment.getProperty(UNPOOLED_VARIABLE));
        if (chosen.isEmpty()) {
            chosen = DatabaseUrl.parse(environment.getProperty(URL_VARIABLE));
        }
        chosen.ifPresent(parts -> {
            Map<String, Object> overrides = new HashMap<>();
            overrides.put("spring.datasource.url", parts.jdbcUrl());
            if (parts.username() != null) {
                overrides.put("spring.datasource.username", parts.username());
            }
            if (parts.password() != null) {
                overrides.put("spring.datasource.password", parts.password());
            }
            environment.getPropertySources().addFirst(new MapPropertySource(SOURCE_NAME, overrides));
        });
    }
}
