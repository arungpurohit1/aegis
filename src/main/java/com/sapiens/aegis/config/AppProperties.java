package com.sapiens.aegis.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Type-safe view of our custom "aegis.*" configuration.
 * Bound automatically from application.properties (@ConfigurationPropertiesScan on the main class).
 */
@ConfigurationProperties(prefix = "aegis")
public class AppProperties {

    private final Correlation correlation = new Correlation();
    private final Jwt jwt = new Jwt();

    public Correlation getCorrelation() {
        return correlation;
    }

    public Jwt getJwt() {
        return jwt;
    }

    /** Maps the "aegis.correlation.*" sub-tree. */
    public static class Correlation {
        /** HTTP header used to read/propagate the request correlation id. */
        private String headerName = "X-Correlation-Id";

        public String getHeaderName() {
            return headerName;
        }

        public void setHeaderName(String headerName) {
            this.headerName = headerName;
        }
    }

    /** Maps the "aegis.jwt.*" sub-tree. */
    public static class Jwt {
        /**
         * HS256 signing key, base64. Dev-only default below; a real deployment
         * must override this via an environment variable, never commit a
         * production secret to a properties file.
         */
        private String secret = "akj8z7/6sN4ux2FgMnbYFdHGeXa211rqa+kODmiJ/vg=";
        private long expirationMinutes = 60;

        public String getSecret() {
            return secret;
        }

        public void setSecret(String secret) {
            this.secret = secret;
        }

        public long getExpirationMinutes() {
            return expirationMinutes;
        }

        public void setExpirationMinutes(long expirationMinutes) {
            this.expirationMinutes = expirationMinutes;
        }
    }
}
