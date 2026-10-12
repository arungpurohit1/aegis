package com.sapiens.aegis.common;

import java.time.Instant;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Our own liveness endpoint from M1, kept alongside actuator's /actuator/health.
 * The path stays /firstRestApi/ping (not /api/v1) on purpose — it's the very first
 * endpoint of the project and the URL is a deliberate keepsake.
 */
@RestController
@RequestMapping("/firstRestApi")
public class HealthController {

    @GetMapping("/ping")
    public Map<String, Object> ping() {
        return Map.of(
            "service", "aegis",
            "status", "UP and Down",
            "timestamp", Instant.now().toString());
    }
}
