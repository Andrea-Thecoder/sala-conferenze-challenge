package com.sala.challenge.integration;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.sala.challenge.dto.auth.LoginCredentialsDTO;
import com.sala.challenge.model.User;
import com.sala.challenge.model.enumerator.Role;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;

@QuarkusTest
class AuthFlowIT extends AbstractIntegrationTest {

    private User seededUser;

    @AfterEach
    void cleanup() {
        if (seededUser != null) {
            deleteUser(seededUser.getId());
            seededUser = null;
        }
    }

    @Test
    void login_activeUserCorrectCredentials_returnsTokenPair() {
        seededUser = seedUser("auth-flow-" + UUID.randomUUID() + "@example.com", Role.CUSTOMER, true);
        LoginCredentialsDTO credentials = new LoginCredentialsDTO();
        credentials.setEmail(seededUser.getEmail());
        credentials.setPassword("Password1!");

        RestAssured.given()
                .contentType(ContentType.JSON)
                .body(credentials)
                .when().post("/auth/login")
                .then()
                .statusCode(200)
                .body("accessToken", notNullValue())
                .body("refreshToken", notNullValue());
    }

    @Test
    void login_inactiveUser_returnsBadRequestWithGenericMessage() {
        seededUser = seedUser("auth-flow-" + UUID.randomUUID() + "@example.com", Role.CUSTOMER, false);
        LoginCredentialsDTO credentials = new LoginCredentialsDTO();
        credentials.setEmail(seededUser.getEmail());
        credentials.setPassword("Password1!");

        RestAssured.given()
                .contentType(ContentType.JSON)
                .body(credentials)
                .when().post("/auth/login")
                .then()
                .statusCode(400)
                .body("violations[0].message", equalTo("Invalid email or password"));
    }

    @Test
    void login_wrongPassword_returnsBadRequestWithGenericMessage() {
        seededUser = seedUser("auth-flow-" + UUID.randomUUID() + "@example.com", Role.CUSTOMER, true);
        LoginCredentialsDTO credentials = new LoginCredentialsDTO();
        credentials.setEmail(seededUser.getEmail());
        credentials.setPassword("WrongPassword1!");

        RestAssured.given()
                .contentType(ContentType.JSON)
                .body(credentials)
                .when().post("/auth/login")
                .then()
                .statusCode(400)
                .body("violations[0].message", equalTo("Invalid email or password"));
    }

    @Test
    void refreshAndLogout_fullSessionLifecycle_bothSucceed() {
        seededUser = seedUser("auth-flow-" + UUID.randomUUID() + "@example.com", Role.CUSTOMER, true);
        LoginCredentialsDTO credentials = new LoginCredentialsDTO();
        credentials.setEmail(seededUser.getEmail());
        credentials.setPassword("Password1!");
        String rawRefreshToken = RestAssured.given()
                .contentType(ContentType.JSON)
                .body(credentials)
                .when().post("/auth/login")
                .then().statusCode(200)
                .extract().path("refreshToken");

        String refreshBody = "{\"refreshToken\":\"" + rawRefreshToken + "\"}";
        String rotatedRefreshToken = RestAssured.given()
                .contentType(ContentType.JSON)
                .body(refreshBody)
                .when().post("/auth/refresh")
                .then().statusCode(200)
                .body("accessToken", notNullValue())
                .extract().path("refreshToken");

        String logoutBody = "{\"refreshToken\":\"" + rotatedRefreshToken + "\"}";
        RestAssured.given()
                .contentType(ContentType.JSON)
                .body(logoutBody)
                .when().post("/auth/logout")
                .then()
                .statusCode(200);
    }
}
