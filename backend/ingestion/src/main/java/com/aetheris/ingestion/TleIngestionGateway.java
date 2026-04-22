package com.aetheris.ingestion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@EnableScheduling
public class TleIngestionGateway {
    private static final Logger logger = LoggerFactory.getLogger(TleIngestionGateway.class);
    private final RestTemplate restTemplate = new RestTemplate();
    
    // CelesTrak active satellites TLE URL
    private static final String CELESTRAK_URL = "https://celestrak.org/NORAD/elements/active.txt";

    @Scheduled(fixedRate = 12 * 60 * 60 * 1000) // Every 12 hours
    public void fetchTles() {
        Thread.ofVirtual().start(() -> {
            try {
                logger.info("Fetching TLE data from CelesTrak...");
                String data = restTemplate.getForObject(CELESTRAK_URL, String.class);
                if (data != null) {
                    int lineCount = data.split("\n").length;
                    logger.info("Successfully fetched TLE data: {} lines", lineCount);
                    // Process TLEs (3-line format) and broadcast to Streaming Core
                }
            } catch (Exception e) {
                logger.error("Failed to fetch TLE data", e);
            }
        });
    }
}
