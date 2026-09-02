package com.naxos.challenge.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.agroal.api.AgroalDataSource;
import io.ebean.Database;
import io.ebean.config.CurrentUserProvider;
import io.ebean.config.DatabaseConfig;
import io.quarkus.runtime.Startup;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

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

    @Singleton
    public ObjectMapper createMapper() {
        ObjectMapper obj = new ObjectMapper();
        obj.registerModule(new JavaTimeModule());
        obj.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        obj.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        return obj;
    }
}
