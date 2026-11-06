package com.advancedswiftly.pawfit.global.config;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pawfit")
public class PawfitProperties {

    private final Cors cors = new Cors();

    public Cors getCors() {
        return cors;
    }

    public static class Cors {

        private List<String> allowedOrigins = new ArrayList<>(
                List.of("http://localhost:3000", "http://localhost:5173"));

        public List<String> getAllowedOrigins() {
            return allowedOrigins;
        }

        public void setAllowedOrigins(List<String> allowedOrigins) {
            this.allowedOrigins = allowedOrigins;
        }
    }
}
