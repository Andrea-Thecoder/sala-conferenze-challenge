package com.sala.challenge.config;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigMapping(prefix = "rate-limit.auth")
public interface RateLimitConfig {

    @WithDefault("5")
    Integer maxAttempts();

    @WithDefault("60")
    Long windowSeconds();

}
