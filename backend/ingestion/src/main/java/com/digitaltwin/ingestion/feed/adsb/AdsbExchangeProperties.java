package com.digitaltwin.ingestion.feed.adsb;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for the ADSBExchange flight feed provider.
 *
 * <p>No API key is required for the public endpoint.
 * Override {@code ADSB_URL} to point at a local mirror or a different endpoint.
 */
@ConfigurationProperties(prefix = "ingestion.adsb")
@Component
public class AdsbExchangeProperties {

    private String url = "https://public-api.adsbexchange.com/VirtualRadar/AircraftList.json";

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
