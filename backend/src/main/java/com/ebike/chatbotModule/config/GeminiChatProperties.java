package com.ebike.chatbotModule.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.ai.gemini")
public class GeminiChatProperties {

    private String apiKey;
    private String model = "gemini-3.6-flash";
    private boolean enabled = true;
    private int connectTimeoutMs = 10000;
    private int readTimeoutMs = 60000;
    private int visionReadTimeoutMs = 45000;
    private int maxAttempts = 3;
    private long retryDelayMs = 3000;

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

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getConnectTimeoutMs() {
        return connectTimeoutMs;
    }

    public void setConnectTimeoutMs(int connectTimeoutMs) {
        this.connectTimeoutMs = connectTimeoutMs;
    }

    public int getReadTimeoutMs() {
        return readTimeoutMs;
    }

    public void setReadTimeoutMs(int readTimeoutMs) {
        this.readTimeoutMs = readTimeoutMs;
    }

    public int getVisionReadTimeoutMs() {
        return visionReadTimeoutMs;
    }

    public void setVisionReadTimeoutMs(int visionReadTimeoutMs) {
        this.visionReadTimeoutMs = visionReadTimeoutMs;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    public long getRetryDelayMs() {
        return retryDelayMs;
    }

    public void setRetryDelayMs(long retryDelayMs) {
        this.retryDelayMs = retryDelayMs;
    }
}
