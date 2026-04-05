package com.digitaltwin.ingestion.feed.aisstream;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for the AISStream.io ship feed provider.
 *
 * <p>Obtain a free API key at <a href="https://aisstream.io">https://aisstream.io</a>
 * and supply it via the {@code AISSTREAM_API_KEY} environment variable.
 */
@ConfigurationProperties(prefix = "ingestion.aisstream")
@Component
public class AisStreamProperties {

    private String apiKey = "";

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }
}
