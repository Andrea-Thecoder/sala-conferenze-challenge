package com.sala.challenge.integration;

import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.sala.challenge.model.User;
import com.sala.challenge.model.enumerator.Role;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;

/**
 * Il gate "ogni nuovo account nasce inattivo, serve un ADMIN per attivarlo"
 * (vedi Javadoc di AuthResource.activateUser) è enforced tra Resource, Service
 * e Repository insieme: solo un test end-to-end su HTTP+DB reali può provare
 * che il login viene davvero negato prima dell'attivazione e concesso dopo.
 */
@QuarkusTest
class RegistrationFlowIT extends AbstractIntegrationTest {

    private User admin;
    private UUID registeredUserId;

    @AfterEach
    void cleanup() {
        if (registeredUserId != null) deleteUser(registeredUserId);
        if (admin != null) deleteUser(admin.getId());
    }

    private String registerNewCustomer(String email) {
        String body = "{"
                + "\"firstName\":\"Mario\","
                + "\"lastName\":\"Rossi\","
                + "\"email\":\"" + email + "\","
                + "\"password\":\"Password1!\","
                + "\"phoneNumber\":\"+393331234567\","
                + "\"role\":\"CUSTOMER\"}";

        String createdId = RestAssured.given()
                .contentType(ContentType.JSON)
                .body(body)
                .when().post("/auth/register")
                .then().statusCode(200)
                .extract().path("payload");

        registeredUserId = UUID.fromString(createdId);
        return createdId;
    }

    @Test
    void register_newAccount_isInactiveAndCannotLogin() {
        String email = "register-" + UUID.randomUUID() + "@example.com";
        registerNewCustomer(email);

        RestAssured.given()
                .contentType(ContentType.JSON)
                .body("{\"email\":\"" + email + "\",\"password\":\"Password1!\"}")
                .when().post("/auth/login")
                .then()
                .statusCode(400);
    }

    @Test
    void register_afterAdminActivation_canLogin() {
        String adminEmail = "register-" + UUID.randomUUID() + "@example.com";
        admin = seedUser(adminEmail, Role.ADMIN, true);
        String adminToken = loginAndGetAccessToken(adminEmail);
        String email = "register-" + UUID.randomUUID() + "@example.com";
        registerNewCustomer(email);

        RestAssured.given()
                .header("Authorization", "Bearer " + adminToken)
                .when().patch("/auth/users/" + registeredUserId + "/activate")
                .then().statusCode(200);

        RestAssured.given()
                .contentType(ContentType.JSON)
                .body("{\"email\":\"" + email + "\",\"password\":\"Password1!\"}")
                .when().post("/auth/login")
                .then()
                .statusCode(200)
                .body("accessToken", org.hamcrest.Matchers.notNullValue());
    }
}
