package com.centsible.backend.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.HashMap;
import java.util.Map;

/**
 * If DATABASE_URL is a {@code postgres://user:password@host/db} URL (the format most hosting
 * and database providers give you), fill in the datasource settings from it, so it can be pasted
 * in as-is. A {@code jdbc:} URL, or no DATABASE_URL at all (local development), changes nothing.
 * See {@link DatabaseUrl} for the conversion rules.
 */
public class DatabaseUrlEnvironmentPostProcessor implements EnvironmentPostProcessor {

    static final String SOURCE_NAME = "centsibleDatabaseUrl";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        DatabaseUrl.parse(environment.getProperty("DATABASE_URL")).ifPresent(parts -> {
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
