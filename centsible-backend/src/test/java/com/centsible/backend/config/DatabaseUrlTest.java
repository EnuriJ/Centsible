package com.centsible.backend.config;

import org.junit.jupiter.api.Test;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The postgres:// URL that hosting providers hand out must become a working JDBC connection. */
public class DatabaseUrlTest {

    @Test
    void convertsAProviderStyleUrl() {
        DatabaseUrl.Parts parts = DatabaseUrl.parse(
                "postgres://app_user:s3cret@db.example.com:5432/centsible?sslmode=require").orElseThrow();

        assertEquals("jdbc:postgresql://db.example.com:5432/centsible?sslmode=require", parts.jdbcUrl());
        assertEquals("app_user", parts.username());
        assertEquals("s3cret", parts.password());
    }

    @Test
    void acceptsThePostgresqlSchemeAndDefaultsThePort() {
        DatabaseUrl.Parts parts = DatabaseUrl.parse("postgresql://u:p@host.example.com/mydb").orElseThrow();

        assertEquals("jdbc:postgresql://host.example.com:5432/mydb?sslmode=require", parts.jdbcUrl());
    }

    @Test
    void decodesSpecialCharactersInThePassword() {
        DatabaseUrl.Parts parts = DatabaseUrl.parse("postgres://u:p%40ss%2Fw%3Ard%2B1@host/db").orElseThrow();

        assertEquals("p@ss/w:rd+1", parts.password());
    }

    @Test
    void aLiteralPlusInThePasswordStaysAPlus() {
        DatabaseUrl.Parts parts = DatabaseUrl.parse("postgres://u:a+b@host/db").orElseThrow();

        assertEquals("a+b", parts.password());
    }

    @Test
    void keepsSslmodeButDropsOptionsTheDriverDoesNotKnow() {
        DatabaseUrl.Parts parts = DatabaseUrl.parse(
                "postgres://u:p@host/db?sslmode=verify-full&channel_binding=require&application_name=x")
                .orElseThrow();

        assertEquals("jdbc:postgresql://host:5432/db?sslmode=verify-full", parts.jdbcUrl());
    }

    @Test
    void ignoresAnUnexpectedSslmodeValue() {
        DatabaseUrl.Parts parts = DatabaseUrl.parse("postgres://u:p@host/db?sslmode=x;DROP").orElseThrow();

        assertTrue(parts.jdbcUrl().endsWith("sslmode=require"));
    }

    @Test
    void leavesJdbcUrlsAndEmptyValuesAlone() {
        assertEquals(Optional.empty(), DatabaseUrl.parse("jdbc:postgresql://host:5432/db"));
        assertEquals(Optional.empty(), DatabaseUrl.parse(""));
        assertEquals(Optional.empty(), DatabaseUrl.parse(null));
    }

    @Test
    void rejectsAUrlWithoutAHostOrDatabase() {
        assertThrows(IllegalArgumentException.class, () -> DatabaseUrl.parse("postgres:///db"));
        assertThrows(IllegalArgumentException.class, () -> DatabaseUrl.parse("postgres://u:p@host"));
    }

    @Test
    void errorMessagesAndToStringNeverContainThePassword() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> DatabaseUrl.parse("postgres://u:topsecret@ho st/db"));
        assertFalse(error.getMessage().contains("topsecret"));

        DatabaseUrl.Parts parts = DatabaseUrl.parse("postgres://u:topsecret@host/db").orElseThrow();
        assertFalse(parts.toString().contains("topsecret"));
    }

    @Test
    void usernameOnlyUrlLeavesPasswordUnset() {
        DatabaseUrl.Parts parts = DatabaseUrl.parse("postgres://onlyuser@host/db").orElseThrow();

        assertEquals("onlyuser", parts.username());
        assertNull(parts.password());
    }

    @Test
    void environmentPostProcessorOverridesTheDatasourceSettings() {
        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().addLast(new MapPropertySource("test",
                Map.of("DATABASE_URL", "postgres://app:pw@db.example.com/centsible")));

        new DatabaseUrlEnvironmentPostProcessor().postProcessEnvironment(environment, null);

        assertEquals("jdbc:postgresql://db.example.com:5432/centsible?sslmode=require",
                environment.getProperty("spring.datasource.url"));
        assertEquals("app", environment.getProperty("spring.datasource.username"));
        assertEquals("pw", environment.getProperty("spring.datasource.password"));
    }

    @Test
    void environmentPostProcessorDoesNothingForJdbcUrlsOrWhenUnset() {
        StandardEnvironment jdbc = new StandardEnvironment();
        jdbc.getPropertySources().addLast(new MapPropertySource("test",
                Map.of("DATABASE_URL", "jdbc:postgresql://host/db")));
        new DatabaseUrlEnvironmentPostProcessor().postProcessEnvironment(jdbc, null);
        assertNull(jdbc.getProperty("spring.datasource.url"));

        StandardEnvironment unset = new StandardEnvironment();
        new DatabaseUrlEnvironmentPostProcessor().postProcessEnvironment(unset, null);
        assertNull(unset.getProperty("spring.datasource.url"));
    }

    @Test
    void prefersTheUnpooledUrlWhenBothAreProvided() {
        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().addLast(new MapPropertySource("test", Map.of(
                "DATABASE_URL", "postgres://app:pooledpw@ep-abc-pooler.example.com/centsible",
                "DATABASE_URL_UNPOOLED", "postgres://app:directpw@ep-abc.example.com/centsible")));

        new DatabaseUrlEnvironmentPostProcessor().postProcessEnvironment(environment, null);

        assertEquals("jdbc:postgresql://ep-abc.example.com:5432/centsible?sslmode=require",
                environment.getProperty("spring.datasource.url"));
        assertEquals("directpw", environment.getProperty("spring.datasource.password"));
    }

    @Test
    void usesDatabaseUrlWhenThereIsNoUnpooledOne() {
        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().addLast(new MapPropertySource("test",
                Map.of("DATABASE_URL", "postgres://app:pw@host.example.com/centsible")));

        new DatabaseUrlEnvironmentPostProcessor().postProcessEnvironment(environment, null);

        assertEquals("jdbc:postgresql://host.example.com:5432/centsible?sslmode=require",
                environment.getProperty("spring.datasource.url"));
    }
}
