package com.sala.challenge.config;
import io.ebean.config.CurrentUserProvider;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.microprofile.jwt.JsonWebToken;


@ApplicationScoped
public class CurrentUserProviderImpl implements CurrentUserProvider {

    @Inject
    JsonWebToken jwt;

    @Override
    public Object currentUser() {
        return getCurrentUser();
    }

    public String getCurrentUser() {
        if(jwt == null || StringUtils.isBlank(jwt.getSubject())){
            return "N/A";
        }
        return jwt.getSubject();
    }
}
