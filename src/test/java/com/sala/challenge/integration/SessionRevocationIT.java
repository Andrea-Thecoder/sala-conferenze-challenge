package com.sala.challenge.integration;

import static org.hamcrest.Matchers.equalTo;

import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.sala.challenge.model.User;
import com.sala.challenge.model.enumerator.Role;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

/**
 * Verifica end-to-end, su Redis vero, il meccanismo per cui un access token già
 * emesso smette di funzionare non appena l'utente viene revocato o fa logout —
 * comportamento impossibile da testare a livello unitario (AccessTokenBlacklistFilter
 * è un filtro JAX-RS globale, mai istanziato nei test con service mockati).
 */
@QuarkusTest
class SessionRevocationIT extends AbstractIntegrationTest {

    private User admin;
    private User target;

    @AfterEach
    void cleanup() {
        if (target != null) deleteUser(target.getId());
        if (admin != null) deleteUser(admin.getId());
    }

    @Test
    void revokeUser_accessTokenIssuedBeforeRevocation_isRejectedOnNextRequest() throws InterruptedException {
        String adminEmail = "revoke-" + UUID.randomUUID() + "@example.com";
        String targetEmail = "revoke-" + UUID.randomUUID() + "@example.com";
        admin = seedUser(adminEmail, Role.ADMIN, true);
        target = seedUser(targetEmail, Role.CUSTOMER, true);
        String adminToken = loginAndGetAccessToken(adminEmail);
        String targetToken = loginAndGetAccessToken(targetEmail);
        // isTokenIssuedBeforeUserRevocation confronta epoch-seconds con "<" stretto:
        // un token emesso nello STESSO secondo della revoca non risulta "prima" (per
        // design, vedi AccessTokenBlacklist), quindi va garantito un confine di secondo
        // reale tra login e revoke perché il test non sia intermittente.
        Thread.sleep(1100);

        RestAssured.given()
                .header("Authorization", "Bearer " + adminToken)
                .when().delete("/auth/users/" + target.getId() + "/revoke")
                .then().statusCode(200);

        RestAssured.given()
                .header("Authorization", "Bearer " + targetToken)
                .when().get("/users/" + target.getId())
                .then()
                .statusCode(401);
    }

    @Test
    void revokeUser_thenLogin_returnsBadRequestWithGenericMessage() {
        String adminEmail = "revoke-" + UUID.randomUUID() + "@example.com";
        String targetEmail = "revoke-" + UUID.randomUUID() + "@example.com";
        admin = seedUser(adminEmail, Role.ADMIN, true);
        target = seedUser(targetEmail, Role.CUSTOMER, true);
        String adminToken = loginAndGetAccessToken(adminEmail);
        RestAssured.given()
                .header("Authorization", "Bearer " + adminToken)
                .when().delete("/auth/users/" + target.getId() + "/revoke")
                .then().statusCode(200);

        RestAssured.given()
                .contentType("application/json")
                .body("{\"email\":\"" + targetEmail + "\",\"password\":\"Password1!\"}")
                .when().post("/auth/login")
                .then()
                .statusCode(400)
                .body("violations[0].message", equalTo("Invalid email or password"));
    }

    @Test
    void logout_accessTokenUsedAfterLogout_isRejected() {
        String email = "revoke-" + UUID.randomUUID() + "@example.com";
        target = seedUser(email, Role.CUSTOMER, true);
        io.restassured.response.Response loginResponse = RestAssured.given()
                .contentType("application/json")
                .body("{\"email\":\"" + email + "\",\"password\":\"Password1!\"}")
                .when().post("/auth/login")
                .then().statusCode(200)
                .extract().response();
        String accessToken = loginResponse.path("accessToken");
        String refreshToken = loginResponse.path("refreshToken");

        RestAssured.given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType("application/json")
                .body("{\"refreshToken\":\"" + refreshToken + "\"}")
                .when().post("/auth/logout")
                .then().statusCode(200);

        RestAssured.given()
                .header("Authorization", "Bearer " + accessToken)
                .when().get("/users/" + target.getId())
                .then()
                .statusCode(401);
    }
}
