package com.centsible.backend.config;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * Turns the connection string most hosted Postgres services hand out
 * ({@code postgres://user:password@host:5432/dbname?sslmode=require}) into what the JDBC driver
 * needs: a {@code jdbc:postgresql://...} URL plus a separate username and password.
 *
 * Only the {@code sslmode} option is carried over. Providers sometimes add options the JDBC
 * driver doesn't know (for example {@code channel_binding}), and passing those through can
 * break the connection. If no {@code sslmode} is given, {@code require} is used, since hosted
 * databases expect an encrypted connection.
 *
 * A value that is already a {@code jdbc:} URL, or anything else, is left alone (empty result).
 */
public final class DatabaseUrl {

    /** The pieces to hand to the datasource. */
    public record Parts(String jdbcUrl, String username, String password) {
        // Keep the password out of logs and exception messages.
        @Override
        public String toString() {
            return "Parts[jdbcUrl=" + jdbcUrl + ", username=" + username + ", password=***]";
        }
    }

    private DatabaseUrl() {
    }

    public static Optional<Parts> parse(String raw) {
        if (raw == null) {
            return Optional.empty();
        }
        String value = raw.trim();
        if (!value.startsWith("postgres://") && !value.startsWith("postgresql://")) {
            return Optional.empty();
        }

        URI uri;
        try {
            uri = new URI(value);
        } catch (Exception e) {
            // Deliberately not including the value: it contains the password.
            throw new IllegalArgumentException("DATABASE_URL is not a valid postgres:// URL");
        }

        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            throw new IllegalArgumentException("DATABASE_URL has no host name");
        }
        int port = uri.getPort() == -1 ? 5432 : uri.getPort();

        String database = uri.getPath() == null ? "" : uri.getPath().replaceFirst("^/", "");
        if (database.isBlank()) {
            throw new IllegalArgumentException("DATABASE_URL has no database name");
        }

        String username = null;
        String password = null;
        String userInfo = uri.getRawUserInfo();
        if (userInfo != null && !userInfo.isEmpty()) {
            int colon = userInfo.indexOf(':');
            username = decode(colon >= 0 ? userInfo.substring(0, colon) : userInfo);
            password = colon >= 0 ? decode(userInfo.substring(colon + 1)) : null;
        }

        String sslMode = "require";
        String query = uri.getRawQuery();
        if (query != null) {
            for (String pair : query.split("&")) {
                int eq = pair.indexOf('=');
                if (eq > 0 && pair.substring(0, eq).equals("sslmode")) {
                    String candidate = decode(pair.substring(eq + 1));
                    if (candidate.matches("[a-z-]+")) {
                        sslMode = candidate;
                    }
                }
            }
        }

        String hostPart = host.contains(":") ? "[" + host + "]" : host; // IPv6 literal
        String jdbcUrl = "jdbc:postgresql://" + hostPart + ":" + port + "/" + database
                + "?sslmode=" + sslMode;
        return Optional.of(new Parts(jdbcUrl, username, password));
    }

    private static String decode(String s) {
        return URLDecoder.decode(s.replace("+", "%2B"), StandardCharsets.UTF_8);
    }
}
