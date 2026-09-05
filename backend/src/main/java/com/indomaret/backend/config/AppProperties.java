package com.indomaret.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

@Configuration
@ConfigurationProperties(prefix = "app")
@Data
public class AppProperties {

    private Pagination pagination = new Pagination();
    private Whitelist whitelist = new Whitelist();
    private Jwt jwt = new Jwt();

    @Data
    public static class Pagination {
        private int defaultPageSize = 20;
        private int maxPageSize = 100;
    }

    @Data
    public static class Whitelist {
        private int maxStoreCount = 50;
    }

    @Data
    public static class Jwt {
        private String secret = "mySecretKeyForJwtTokenGenerationThatIsLongEnoughForHS256Algorithm2024";
        private long expirationMs = 3600000;
    }
}
