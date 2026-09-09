package com.sala.challenge.integration;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.sala.challenge.dto.booking.ConferenceHallReservationDTO;
import com.sala.challenge.dto.booking.CreateBookingDTO;
import com.sala.challenge.model.Building;
import com.sala.challenge.model.ConferenceHall;
import com.sala.challenge.model.User;
import com.sala.challenge.model.enumerator.Role;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Il problema originale che ha motivato tutto il lavoro su booking overlap in
 * questo progetto: due richieste concorrenti sulla stessa sala/slot devono
 * risolversi in un solo successo, mai due prenotazioni sovrapposte. Con i
 * repository sempre mockati, nessun unit test può davvero esercitare una race
 * condition reale — serve il vincolo EXCLUDE di Postgres sotto carico vero.
 */
@QuarkusTest
class BookingOverlapRaceConditionIT extends AbstractIntegrationTest {

    private User customer;
    private Building building;
    private ConferenceHall hall;

    @AfterEach
    void cleanup() {
        if (hall != null) deleteConferenceHall(hall.getId());
        if (building != null) deleteBuilding(building.getId());
        if (customer != null) deleteUser(customer.getId());
    }

    @Test
    void createBookings_twoConcurrentRequestsSameSlot_exactlyOneSucceeds() throws InterruptedException {
        String email = "overlap-" + UUID.randomUUID() + "@example.com";
        customer = seedUser(email, Role.CUSTOMER, true);
        building = seedBuilding();
        hall = seedConferenceHall(building, true);
        String token = loginAndGetAccessToken(email);

        LocalDateTime start = LocalDateTime.now().plusDays(2).withNano(0);
        LocalDateTime end = start.plusHours(1);
        CreateBookingDTO dto = new CreateBookingDTO();
        dto.setConferenceHallReservationDTOs(List.of(
                new ConferenceHallReservationDTO(hall.getId(), start, end)
        ));

        CountDownLatch startLatch = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            List<Future<Response>> futures = List.of(
                    executor.submit(() -> fireBookingRequest(token, dto, startLatch)),
                    executor.submit(() -> fireBookingRequest(token, dto, startLatch))
            );
            startLatch.countDown();

            int totalSuccesses = 0;
            int totalFailures = 0;
            for (Future<Response> future : futures) {
                Response response = future.get(10, TimeUnit.SECONDS);
                assertThat(response.getStatusCode()).isEqualTo(200);
                totalSuccesses += response.<Integer>path("payload.insertSuccessCount");
                totalFailures += response.<Integer>path("payload.insertFailureCount");
            }

            assertThat(totalSuccesses).isEqualTo(1);
            assertThat(totalFailures).isEqualTo(1);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            executor.shutdownNow();
        }
    }

    private Response fireBookingRequest(String token, CreateBookingDTO dto, CountDownLatch startLatch) {
        try {
            startLatch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return RestAssured.given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(dto)
                .when().post("/bookings");
    }
}
