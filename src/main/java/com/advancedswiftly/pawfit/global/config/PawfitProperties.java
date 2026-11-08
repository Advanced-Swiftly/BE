package com.advancedswiftly.pawfit.global.config;

import com.advancedswiftly.pawfit.global.ai.AiProvider;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pawfit")
public class PawfitProperties {

    private final Cors cors = new Cors();
    private final Storage storage = new Storage();
    private final Ai ai = new Ai();

    public Cors getCors() {
        return cors;
    }

    public Storage getStorage() {
        return storage;
    }

    public Ai getAi() {
        return ai;
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

    public static class Storage {

        private String url = "";
        private String serviceRoleKey = "";
        private String bucket = "pawfit";
        private long maxFileSize = 10 * 1024 * 1024;
        private List<String> allowedContentTypes = new ArrayList<>(
                List.of("image/jpeg", "image/png", "image/webp", "application/pdf"));
        private Duration signedUrlTtl = Duration.ofMinutes(5);
        private Duration timeout = Duration.ofSeconds(10);

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getServiceRoleKey() {
            return serviceRoleKey;
        }

        public void setServiceRoleKey(String serviceRoleKey) {
            this.serviceRoleKey = serviceRoleKey;
        }

        public String getBucket() {
            return bucket;
        }

        public void setBucket(String bucket) {
            this.bucket = bucket;
        }

        public long getMaxFileSize() {
            return maxFileSize;
        }

        public void setMaxFileSize(long maxFileSize) {
            this.maxFileSize = maxFileSize;
        }

        public List<String> getAllowedContentTypes() {
            return allowedContentTypes;
        }

        public void setAllowedContentTypes(List<String> allowedContentTypes) {
            this.allowedContentTypes = allowedContentTypes;
        }

        public Duration getSignedUrlTtl() {
            return signedUrlTtl;
        }

        public void setSignedUrlTtl(Duration signedUrlTtl) {
            this.signedUrlTtl = signedUrlTtl;
        }

        public Duration getTimeout() {
            return timeout;
        }

        public void setTimeout(Duration timeout) {
            this.timeout = timeout;
        }
    }

    public static class Ai {

        private AiProvider provider = AiProvider.GEMINI;
        private Duration timeout = Duration.ofSeconds(30);
        private int maxRetries = 2;
        private Duration retryDelay = Duration.ofMillis(500);
        private final Gemini gemini = new Gemini();
        private final WorkersAi workersAi = new WorkersAi();

        public AiProvider getProvider() {
            return provider;
        }

        public void setProvider(AiProvider provider) {
            this.provider = provider;
        }

        public Duration getTimeout() {
            return timeout;
        }

        public void setTimeout(Duration timeout) {
            this.timeout = timeout;
        }

        public int getMaxRetries() {
            return maxRetries;
        }

        public void setMaxRetries(int maxRetries) {
            this.maxRetries = maxRetries;
        }

        public Duration getRetryDelay() {
            return retryDelay;
        }

        public void setRetryDelay(Duration retryDelay) {
            this.retryDelay = retryDelay;
        }

        public Gemini getGemini() {
            return gemini;
        }

        public WorkersAi getWorkersAi() {
            return workersAi;
        }
    }

    public static class Gemini {

        private String apiKey = "";
        private String model = "gemini-3.8-flash";

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }
    }

    public static class WorkersAi {

        private String accountId = "";
        private String apiToken = "";
        private String textModel = "@cf/meta/llama-3.1-8b-instruct";
        private String visionModel = "@cf/meta/llama-3.2-11b-vision-instruct";

        public String getAccountId() {
            return accountId;
        }

        public void setAccountId(String accountId) {
            this.accountId = accountId;
        }

        public String getApiToken() {
            return apiToken;
        }

        public void setApiToken(String apiToken) {
            this.apiToken = apiToken;
        }

        public String getTextModel() {
            return textModel;
        }

        public void setTextModel(String textModel) {
            this.textModel = textModel;
        }

        public String getVisionModel() {
            return visionModel;
        }

        public void setVisionModel(String visionModel) {
            this.visionModel = visionModel;
        }
    }
}
