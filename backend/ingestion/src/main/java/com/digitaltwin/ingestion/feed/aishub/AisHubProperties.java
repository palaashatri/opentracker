package com.digitaltwin.ingestion.feed.aishub;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for the AISHub ship feed provider.
 *
 * <p>Obtain a free account at <a href="https://www.aishub.net">https://www.aishub.net</a>
 * and supply your username via the {@code AISHUB_USERNAME} environment variable.
 */
@ConfigurationProperties(prefix = "ingestion.aishub")
@Component
public class AisHubProperties {

    private String username = "";
    private String url = "https://data.aishub.net/ws.php";

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
