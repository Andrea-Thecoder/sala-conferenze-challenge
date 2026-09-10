package com.sala.challenge.integration;

import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.sala.challenge.model.User;
import com.sala.challenge.model.enumerator.Role;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.startsWith;

/**
 * Copre end-to-end la GDPR-style erasure di DELETE /users/{userId}
 * (AuthService.deleteUser -> UserService.anonymizeUser): gli unit test su
 * anonymizeUser verificano solo la scrittura sull'entity mockata, non che
 * l'endpoint reale la esponga correttamente né che una sessione già emessa
 * smetta davvero di funzionare dopo, cosa che richiede Redis vero.
 */
@QuarkusTest
class UserDeletionIT extends AbstractIntegrationTest {

    private User admin;
    private User target;

    @AfterEach
    void cleanup() {
        if (target != null) deleteUser(target.getId());
        if (admin != null) deleteUser(admin.getId());
    }

    @Test
    void deleteUser_asAdmin_anonymizesIdentifyingData() {
        String adminEmail = "del-" + UUID.randomUUID() + "@example.com";
        String targetEmail = "del-" + UUID.randomUUID() + "@example.com";
        admin = seedUser(adminEmail, Role.ADMIN, true);
        target = seedUser(targetEmail, Role.CUSTOMER, true);
        String adminToken = loginAndGetAccessToken(adminEmail);

        RestAssured.given()
                .header("Authorization", "Bearer " + adminToken)
                .when().delete("/users/" + target.getId())
                .then().statusCode(200);

        RestAssured.given()
                .header("Authorization", "Bearer " + adminToken)
                .when().get("/users/" + target.getId())
                .then()
                .statusCode(200)
                .body("firstName", equalTo("User"))
                .body("lastName", equalTo("Deleted"))
                .body("email", startsWith("deleted+"))
                .body("phoneNumber", equalTo("0000000000"))
                .body("role", equalTo("REVOKED"))
                .body("active", equalTo(false));
    }

    @Test
    void deleteUser_asAdmin_makesOriginalCredentialsUnusableForLogin() {
        String adminEmail = "del-" + UUID.randomUUID() + "@example.com";
        String targetEmail = "del-" + UUID.randomUUID() + "@example.com";
        admin = seedUser(adminEmail, Role.ADMIN, true);
        target = seedUser(targetEmail, Role.CUSTOMER, true);
        String adminToken = loginAndGetAccessToken(adminEmail);
        RestAssured.given()
                .header("Authorization", "Bearer " + adminToken)
                .when().delete("/users/" + target.getId())
                .then().statusCode(200);

        RestAssured.given()
                .contentType(ContentType.JSON)
                .body("{\"email\":\"" + targetEmail + "\",\"password\":\"Password1!\"}")
                .when().post("/auth/login")
                .then()
                .statusCode(400)
                .body("violations[0].message", equalTo("Invalid email or password"));
    }

    @Test
    void deleteUser_asAdmin_invalidatesAccessTokenIssuedBeforeDeletion() throws InterruptedException {
        String adminEmail = "del-" + UUID.randomUUID() + "@example.com";
        String targetEmail = "del-" + UUID.randomUUID() + "@example.com";
        admin = seedUser(adminEmail, Role.ADMIN, true);
        target = seedUser(targetEmail, Role.CUSTOMER, true);
        String adminToken = loginAndGetAccessToken(adminEmail);
        String targetToken = loginAndGetAccessToken(targetEmail);
        // Stesso vincolo di SessionRevocationIT: isTokenIssuedBeforeUserRevocation usa "<"
        // stretto sull'epoch-second, quindi serve un confine di secondo reale tra login e
        // delete perché il test non sia intermittente.
        Thread.sleep(1100);

        RestAssured.given()
                .header("Authorization", "Bearer " + adminToken)
                .when().delete("/users/" + target.getId())
                .then().statusCode(200);

        RestAssured.given()
                .header("Authorization", "Bearer " + targetToken)
                .when().get("/users/" + target.getId())
                .then()
                .statusCode(401);
    }
}
