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
 * activateUser/changeRole/revokeUser/revokeAllSessions sono @RolesAllowed("ADMIN")
 * su AuthResource — un ORGANIZER non deve poter passare, nonostante possa
 * gestire conference hall/booking. revokeMySessions invece è self-service
 * (@Authenticated, controllo subject nel service): un CUSTOMER che tenta di
 * revocare le sessioni di un altro utente deve ricevere un errore applicativo
 * (400), non un 403 di RBAC.
 */
@QuarkusTest
class AuthAdminActionsSecurityIT extends AbstractIntegrationTest {

    private User organizer;
    private User customer;
    private User otherCustomer;

    @AfterEach
    void cleanup() {
        if (organizer != null) deleteUser(organizer.getId());
        if (customer != null) deleteUser(customer.getId());
        if (otherCustomer != null) deleteUser(otherCustomer.getId());
    }

    @Test
    void activateUser_asOrganizer_returnsForbidden() {
        String email = "adm-" + UUID.randomUUID() + "@example.com";
        organizer = seedUser(email, Role.ORGANIZER, true);
        customer = seedUser("adm-" + UUID.randomUUID() + "@example.com", Role.CUSTOMER, false);
        String token = loginAndGetAccessToken(email);

        RestAssured.given()
                .header("Authorization", "Bearer " + token)
                .when().patch("/auth/users/" + customer.getId() + "/activate")
                .then()
                .statusCode(403);
    }

    @Test
    void changeRole_asOrganizer_returnsForbidden() {
        String email = "adm-" + UUID.randomUUID() + "@example.com";
        organizer = seedUser(email, Role.ORGANIZER, true);
        customer = seedUser("adm-" + UUID.randomUUID() + "@example.com", Role.CUSTOMER, true);
        String token = loginAndGetAccessToken(email);

        RestAssured.given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body("{\"role\":\"ORGANIZER\"}")
                .when().patch("/auth/users/" + customer.getId() + "/role")
                .then()
                .statusCode(403);
    }

    @Test
    void revokeUser_asOrganizer_returnsForbidden() {
        String email = "adm-" + UUID.randomUUID() + "@example.com";
        organizer = seedUser(email, Role.ORGANIZER, true);
        customer = seedUser("adm-" + UUID.randomUUID() + "@example.com", Role.CUSTOMER, true);
        String token = loginAndGetAccessToken(email);

        RestAssured.given()
                .header("Authorization", "Bearer " + token)
                .when().delete("/auth/users/" + customer.getId() + "/revoke")
                .then()
                .statusCode(403);
    }

    @Test
    void revokeAllSessions_asOrganizer_returnsForbidden() {
        String email = "adm-" + UUID.randomUUID() + "@example.com";
        organizer = seedUser(email, Role.ORGANIZER, true);
        customer = seedUser("adm-" + UUID.randomUUID() + "@example.com", Role.CUSTOMER, true);
        String token = loginAndGetAccessToken(email);

        RestAssured.given()
                .header("Authorization", "Bearer " + token)
                .when().delete("/auth/admin/users/" + customer.getId() + "/sessions")
                .then()
                .statusCode(403);
    }

    @Test
    void revokeMySessions_customerTargetingAnotherUser_returnsBadRequest() {
        String email = "adm-" + UUID.randomUUID() + "@example.com";
        customer = seedUser(email, Role.CUSTOMER, true);
        otherCustomer = seedUser("adm-" + UUID.randomUUID() + "@example.com", Role.CUSTOMER, true);
        String token = loginAndGetAccessToken(email);

        RestAssured.given()
                .header("Authorization", "Bearer " + token)
                .when().delete("/auth/users/" + otherCustomer.getId() + "/sessions")
                .then()
                .statusCode(400);
    }

    @Test
    void revokeMySessions_customerTargetingSelf_returnsSuccess() {
        String email = "adm-" + UUID.randomUUID() + "@example.com";
        customer = seedUser(email, Role.CUSTOMER, true);
        String token = loginAndGetAccessToken(email);

        RestAssured.given()
                .header("Authorization", "Bearer " + token)
                .when().delete("/auth/users/" + customer.getId() + "/sessions")
                .then()
                .statusCode(200);
    }
}
