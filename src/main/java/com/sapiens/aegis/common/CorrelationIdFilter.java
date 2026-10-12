package com.sapiens.aegis.common;

import java.io.IOException;
import java.util.UUID;

import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.sapiens.aegis.config.AppProperties;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Servlet filter that runs on every request (GET, POST, PUT, DELETE, actuator...).
 * Ensures each one has a correlation id:
 *  - reuse the incoming header if the caller sent one (cross-service tracing)
 *  - otherwise generate a fresh UUID
 * The id is placed in SLF4J's MDC so Logback stamps it on every log line for this
 * request, echoed back in the response header, and (since M4) included in error
 * response bodies so a user can quote it in a bug report.
 */
@Component
// Must run before the Spring Security chain (order -100), so even 401/403
// responses written by security carry a correlation id.
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

    /** MDC key holding the current request's correlation id. */
    public static final String MDC_KEY = "traceId";

    private final AppProperties properties;

    public CorrelationIdFilter(AppProperties properties) {
        this.properties = properties;
    }

    /** @return the correlation id of the request being handled, or null outside a request. */
    public static String currentCorrelationId() {
        return MDC.get(MDC_KEY);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String headerName = properties.getCorrelation().getHeaderName();
        String correlationId = request.getHeader(headerName);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }

        MDC.put(MDC_KEY, correlationId);
        response.setHeader(headerName, correlationId);
        try {
            chain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_KEY); // critical: clear per-thread state (threads are pooled/reused)
        }
    }
}
