package com.digitaltwin.ingestion.feed.opensky;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for the OpenSky Network flight feed provider.
 *
 * <p>Anonymous access is used when username is empty. Authenticated users
 * receive a higher rate limit (100 req/10min vs 400 req/10min).
 * Set {@code OPENSKY_USERNAME} and {@code OPENSKY_PASSWORD} environment variables
 * to enable authenticated access.
 */
@ConfigurationProperties(prefix = "ingestion.opensky")
@Component
public class OpenSkyProperties {

    private String username = "";
    private String password = "";

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
