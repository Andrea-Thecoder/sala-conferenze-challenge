package com.sala.challenge.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.agroal.api.AgroalDataSource;
import io.ebean.Database;
import io.ebean.config.CurrentUserProvider;
import io.ebean.config.DatabaseConfig;
import io.quarkus.runtime.Startup;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

public class EbeanConfig {
    @ApplicationScoped
    @Startup
    @Produces
    public Database createDb(AgroalDataSource source, ObjectMapper mapper, CurrentUserProvider currentUserProvider) {
        DatabaseConfig dbConfig = new DatabaseConfig();
        dbConfig.setDataSource(source);
        dbConfig.setCurrentUserProvider(currentUserProvider);
        dbConfig.setObjectMapper(mapper);
        dbConfig.setIdGeneratorAutomatic(false);
        dbConfig.setDefaultServer(true);

        return dbConfig.build();
    }
}
