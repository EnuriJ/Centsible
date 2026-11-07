package com.centsible.backend.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Public "is the server up?" check. It does no work and touches no data. The sign-in page calls
 * it as soon as it loads, so a hosting platform that has put the backend to sleep starts waking
 * it while the visitor is still reading the page.
 */
@RestController
@RequestMapping("/api/health")
public class HealthController {

    @GetMapping
    public Map<String, String> health() {
        return Map.of("status", "ok");
    }
}
