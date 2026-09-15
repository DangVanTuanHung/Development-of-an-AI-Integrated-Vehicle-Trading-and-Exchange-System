package com.ebike.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.storage.s3")
public class S3StorageProperties {

    private String bucket = "kinetic-s3-bucket";
    private String region = "ap-southeast-1";
    private String publicBaseUrl = "";
    private String productImagePrefix = "products";
    private String endpoint = "";
    private String accessKey = "";
    private String secretKey = "";
    private boolean pathStyleAccess = false;
    private boolean initializeBucket = false;

    public String getBucket() {
        return bucket;
    }

    public void setBucket(String bucket) {
        this.bucket = bucket;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getPublicBaseUrl() {
        return publicBaseUrl;
    }

    public void setPublicBaseUrl(String publicBaseUrl) {
        this.publicBaseUrl = publicBaseUrl;
    }

    public String getProductImagePrefix() {
        return productImagePrefix;
    }

    public void setProductImagePrefix(String productImagePrefix) {
        this.productImagePrefix = productImagePrefix;
    }

    public String getEndpoint() { return endpoint; }
    public void setEndpoint(String endpoint) { this.endpoint = endpoint; }
    public String getAccessKey() { return accessKey; }
    public void setAccessKey(String accessKey) { this.accessKey = accessKey; }
    public String getSecretKey() { return secretKey; }
    public void setSecretKey(String secretKey) { this.secretKey = secretKey; }
    public boolean isPathStyleAccess() { return pathStyleAccess; }
    public void setPathStyleAccess(boolean pathStyleAccess) { this.pathStyleAccess = pathStyleAccess; }
    public boolean isInitializeBucket() { return initializeBucket; }
    public void setInitializeBucket(boolean initializeBucket) { this.initializeBucket = initializeBucket; }
}
