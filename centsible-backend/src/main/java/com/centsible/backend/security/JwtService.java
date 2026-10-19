package com.centsible.backend.security;

import com.centsible.backend.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SecurityException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);

    private final String secret;
    private final long expirationMs;

    public JwtService(
            @Value("${centsible.jwt.secret}") String secret,
            @Value("${centsible.jwt.expiration-ms:86400000}") long expirationMs,
            // Checked separately (vs. the resolved secret) purely to detect whether the env
            // var was actually set, so we can warn when the public built-in dev key is in use.
            @Value("${JWT_SECRET:}") String jwtSecretEnvVar) {
        this.secret = secret;
        this.expirationMs = expirationMs;
        if (jwtSecretEnvVar == null || jwtSecretEnvVar.isBlank()) {
            log.warn("=================================================================");
            log.warn(" JWT_SECRET is not set - falling back to the built-in local-dev ");
            log.warn(" signing key. That key is committed to the public repo, so anyone ");
            log.warn(" can forge valid tokens with it. This is fine for local development ");
            log.warn(" ONLY. Before deploying anywhere reachable by others, set the ");
            log.warn(" JWT_SECRET environment variable to a unique, random value. ");
            log.warn("=================================================================");
        }
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("displayName", user.getDisplayName());
        return createToken(claims, user.getEmail());
    }

    private String createToken(Map<String, Object> claims, String subject) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(getSigningKey())
                .compact();
    }

    public String extractEmail(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Long extractUserId(String token) {
        Claims claims = extractAllClaims(token);
        Number userIdNumber = claims.get("userId", Number.class);
        return userIdNumber != null ? userIdNumber.longValue() : null;
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    public Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        try {
            final String email = extractEmail(token);
            return (email != null && email.equalsIgnoreCase(userDetails.getUsername()) && !isTokenExpired(token));
        } catch (SecurityException | MalformedJwtException | ExpiredJwtException | UnsupportedJwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public boolean isTokenExpired(String token) {
        final Date expiration = extractClaim(token, Claims::getExpiration);
        return expiration.before(new Date());
    }
}
