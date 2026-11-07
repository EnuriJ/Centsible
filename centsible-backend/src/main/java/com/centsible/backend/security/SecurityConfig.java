package com.centsible.backend.security;

import com.centsible.backend.config.DataSeeder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final RateLimitFilter rateLimitFilter;
    private final boolean h2ConsoleEnabled;
    private final List<String> allowedOrigins;

    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthFilter,
            RateLimitFilter rateLimitFilter,
            @Value("${spring.h2.console.enabled:false}") boolean h2ConsoleEnabled,
            // Comma-separated list of allowed frontend origins. Defaults cover local dev;
            // set CORS_ALLOWED_ORIGINS in production to the deployed frontend's URL(s).
            @Value("${centsible.cors.allowed-origins:http://localhost:5173,http://localhost:3000}") String allowedOriginsRaw) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.rateLimitFilter = rateLimitFilter;
        this.h2ConsoleEnabled = h2ConsoleEnabled;
        this.allowedOrigins = Arrays.stream(allowedOriginsRaw.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .toList();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> {
                    // Permit preflight OPTIONS requests, auth endpoints and the health check
                    auth.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll();
                    auth.requestMatchers("/api/auth/**", "/error").permitAll();
                    auth.requestMatchers(HttpMethod.GET, "/api/health").permitAll();

                    // Only permit H2 console if explicitly enabled in properties
                    if (h2ConsoleEnabled) {
                        auth.requestMatchers("/h2-console/**").permitAll();
                    }

                    // The shared demo account is read-only: any request that could change state
                    // needs a signed-in user who is NOT the demo user. This is enforced here, on
                    // the server, so it holds no matter what the frontend does. (Anonymous callers
                    // are denied too, which yields 401; the demo user gets 403.)
                    auth.requestMatchers(SecurityConfig::isStateChangingRequest)
                            .access((authentication, context) ->
                                    new AuthorizationDecision(isSignedInNonDemoUser(authentication)));

                    // All other API requests require authentication
                    auth.anyRequest().authenticated();
                })
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType("application/json");
                            response.getWriter().write("{\"error\":\"Unauthorized\",\"message\":\"" + authException.getMessage() + "\"}");
                        })
                        // The only rule that denies an already signed-in user is the demo
                        // read-only rule above, so the 403 message speaks to that.
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            response.setContentType("application/json");
                            response.getWriter().write("{\"error\":\"The demo account is read-only. "
                                    + "Create your own account to upload and edit data.\"}");
                        }))
                // jwtAuthFilter must be registered first - addFilterBefore can only
                // position a new filter relative to one that already has a registered
                // order, and a custom filter class only gets one once it's been added.
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(rateLimitFilter, JwtAuthenticationFilter.class);

        // Allow frames for H2 console only when enabled, otherwise deny
        if (h2ConsoleEnabled) {
            http.headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin));
        } else {
            http.headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::deny));
        }

        return http.build();
    }

    private static final Set<String> SAFE_METHODS = Set.of("GET", "HEAD", "OPTIONS", "TRACE");

    private static boolean isStateChangingRequest(HttpServletRequest request) {
        return !SAFE_METHODS.contains(request.getMethod().toUpperCase());
    }

    private static boolean isSignedInNonDemoUser(Supplier<Authentication> authentication) {
        Authentication auth = authentication.get();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return false;
        }
        boolean isDemo = auth.getPrincipal() instanceof CustomUserDetails details
                && DataSeeder.DEMO_EMAIL.equalsIgnoreCase(details.getUsername());
        return !isDemo;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(allowedOrigins);
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setExposedHeaders(List.of("Authorization"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
