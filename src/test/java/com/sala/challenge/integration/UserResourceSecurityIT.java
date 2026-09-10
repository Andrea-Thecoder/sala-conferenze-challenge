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
 * Copre l'autorizzazione self-vs-admin di UserResource su HTTP reale: gli unit
 * test sul solo Resource (service mockato) non possono verificare che
 * @RolesAllowed blocchi davvero, né che JwtInspector.checkAccessAllowed/sameSubject
 * confrontino il subject reale del JWT emesso da un vero login.
 */
@QuarkusTest
class UserResourceSecurityIT extends AbstractIntegrationTest {

    private User customer;
    private User otherCustomer;
    private User admin;

    @AfterEach
    void cleanup() {
        if (customer != null) deleteUser(customer.getId());
        if (otherCustomer != null) deleteUser(otherCustomer.getId());
        if (admin != null) deleteUser(admin.getId());
    }

    @Test
    void updatePhoneNumber_customerTargetingAnotherUser_returnsForbidden() {
        String email = "usr-" + UUID.randomUUID() + "@example.com";
        customer = seedUser(email, Role.CUSTOMER, true);
        otherCustomer = seedUser("usr-" + UUID.randomUUID() + "@example.com", Role.CUSTOMER, true);
        String token = loginAndGetAccessToken(email);

        RestAssured.given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body("{\"phoneNumber\":\"+393339999999\"}")
                .when().patch("/users/" + otherCustomer.getId() + "/phone-number")
                .then()
                .statusCode(403);
    }

    @Test
    void updatePhoneNumber_customerTargetingSelf_returnsSuccess() {
        String email = "usr-" + UUID.randomUUID() + "@example.com";
        customer = seedUser(email, Role.CUSTOMER, true);
        String token = loginAndGetAccessToken(email);

        RestAssured.given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body("{\"phoneNumber\":\"+393339999999\"}")
                .when().patch("/users/" + customer.getId() + "/phone-number")
                .then()
                .statusCode(200);
    }

    @Test
    void updatePhoneNumber_adminTargetingAnotherUser_returnsSuccess() {
        String adminEmail = "usr-" + UUID.randomUUID() + "@example.com";
        admin = seedUser(adminEmail, Role.ADMIN, true);
        customer = seedUser("usr-" + UUID.randomUUID() + "@example.com", Role.CUSTOMER, true);
        String adminToken = loginAndGetAccessToken(adminEmail);

        RestAssured.given()
                .header("Authorization", "Bearer " + adminToken)
                .contentType(ContentType.JSON)
                .body("{\"phoneNumber\":\"+393338888888\"}")
                .when().patch("/users/" + customer.getId() + "/phone-number")
                .then()
                .statusCode(200);
    }

    @Test
    void changePassword_adminTargetingAnotherUser_returnsForbidden() {
        String adminEmail = "usr-" + UUID.randomUUID() + "@example.com";
        admin = seedUser(adminEmail, Role.ADMIN, true);
        customer = seedUser("usr-" + UUID.randomUUID() + "@example.com", Role.CUSTOMER, true);
        String adminToken = loginAndGetAccessToken(adminEmail);

        RestAssured.given()
                .header("Authorization", "Bearer " + adminToken)
                .contentType(ContentType.JSON)
                .body("{\"currentPassword\":\"Password1!\",\"newPassword\":\"NewPass1!\"}")
                .when().patch("/users/" + customer.getId() + "/password")
                .then()
                .statusCode(403);
    }

    @Test
    void getUserById_customerTargetingAnotherUser_returnsForbidden() {
        String email = "usr-" + UUID.randomUUID() + "@example.com";
        customer = seedUser(email, Role.CUSTOMER, true);
        otherCustomer = seedUser("usr-" + UUID.randomUUID() + "@example.com", Role.CUSTOMER, true);
        String token = loginAndGetAccessToken(email);

        RestAssured.given()
                .header("Authorization", "Bearer " + token)
                .when().get("/users/" + otherCustomer.getId())
                .then()
                .statusCode(403);
    }

    @Test
    void findAllUsers_asCustomer_returnsForbidden() {
        String email = "usr-" + UUID.randomUUID() + "@example.com";
        customer = seedUser(email, Role.CUSTOMER, true);
        String token = loginAndGetAccessToken(email);

        RestAssured.given()
                .header("Authorization", "Bearer " + token)
                .when().get("/users")
                .then()
                .statusCode(403);
    }

    @Test
    void deleteUser_asCustomer_returnsForbidden() {
        String email = "usr-" + UUID.randomUUID() + "@example.com";
        customer = seedUser(email, Role.CUSTOMER, true);
        otherCustomer = seedUser("usr-" + UUID.randomUUID() + "@example.com", Role.CUSTOMER, true);
        String token = loginAndGetAccessToken(email);

        RestAssured.given()
                .header("Authorization", "Bearer " + token)
                .when().delete("/users/" + otherCustomer.getId())
                .then()
                .statusCode(403);
    }
}
