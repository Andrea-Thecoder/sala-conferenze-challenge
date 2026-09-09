package com.sala.challenge.config;


import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigMapping(prefix = "auth")
public interface AuthConfig {

    @WithDefault("10")
    Integer bcryptRounds();

    String jwtIssuer();

    @WithDefault("5")
    Long jwtExpirationMinutes();

    @WithDefault("60")
    Long jwtRefreshExpirationMinutes();

}
