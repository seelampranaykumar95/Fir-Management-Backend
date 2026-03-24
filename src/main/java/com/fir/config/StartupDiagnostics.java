package com.fir.config;

import java.net.URI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class StartupDiagnostics implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(StartupDiagnostics.class);

    private final Environment environment;

    public StartupDiagnostics(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void run(ApplicationArguments args) {
        String[] activeProfiles = environment.getActiveProfiles();
        String profileSummary = activeProfiles.length == 0 ? "default" : String.join(",", activeProfiles);
        String port = environment.getProperty("local.server.port", environment.getProperty("server.port", "8080"));
        String datasourceUrl = environment.getProperty("spring.datasource.url", "");
        String dbHost = extractHost(datasourceUrl);
        String appVersion = getClass().getPackage().getImplementationVersion();

        log.info(
                "Backend runtime ready profile={} port={} dbHost={} version={}",
                profileSummary,
                port,
                dbHost,
                appVersion == null ? "dev" : appVersion);
    }

    private String extractHost(String datasourceUrl) {
        if (datasourceUrl == null || datasourceUrl.isBlank()) {
            return "unknown";
        }

        try {
            String normalized = datasourceUrl.replaceFirst("^jdbc:", "");
            URI uri = URI.create(normalized);
            return uri.getHost() == null ? "unknown" : uri.getHost();
        } catch (IllegalArgumentException ex) {
            return "unknown";
        }
    }
}
