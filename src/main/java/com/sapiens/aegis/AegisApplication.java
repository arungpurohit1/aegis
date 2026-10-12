package com.sapiens.aegis;

import java.util.TimeZone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan  // discovers @ConfigurationProperties beans (AppProperties)
public class AegisApplication {

    static {
        // Windows/JDK reports the host default zone as the legacy alias
        // "Asia/Calcutta", which Postgres's tzdata no longer recognizes and
        // rejects at connection time. A static initializer (rather than the
        // first line of main) also covers tests, which boot the context via
        // SpringApplication.run() directly and never call main().
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }

    public static void main(String[] args) {
        SpringApplication.run(AegisApplication.class, args);
    }
}
