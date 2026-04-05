package com.digitaltwin.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "gateway")
@Component
public class GatewayProperties {

    private String geospatialUrl = "http://localhost:8083";
    private String apiKey = "dev-key";
    private Cors cors = new Cors();

    public String getGeospatialUrl() {
        return geospatialUrl;
    }

    public void setGeospatialUrl(String geospatialUrl) {
        this.geospatialUrl = geospatialUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public Cors getCors() {
        return cors;
    }

    public void setCors(Cors cors) {
        this.cors = cors;
    }

    public static class Cors {

        private String allowedOrigins = "http://localhost:4200";

        public String getAllowedOrigins() {
            return allowedOrigins;
        }

        public void setAllowedOrigins(String allowedOrigins) {
            this.allowedOrigins = allowedOrigins;
        }
    }
}
