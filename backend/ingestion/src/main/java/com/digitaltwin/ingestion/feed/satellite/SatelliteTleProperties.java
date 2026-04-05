package com.digitaltwin.ingestion.feed.satellite;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for the Celestrak TLE satellite feed provider.
 *
 * <p>TLEs are fetched from Celestrak every hour. Override {@code CELESTRAK_TLE_URL}
 * to use a different group or a local mirror.
 * Set {@code SATELLITES_ENABLED=false} to disable satellite propagation entirely.
 */
@ConfigurationProperties(prefix = "ingestion.satellite")
@Component
public class SatelliteTleProperties {

    private boolean enabled = true;
    private String tleUrl = "https://celestrak.org/NORAD/elements/gp.php?GROUP=active&FORMAT=tle";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getTleUrl() {
        return tleUrl;
    }

    public void setTleUrl(String tleUrl) {
        this.tleUrl = tleUrl;
    }
}
