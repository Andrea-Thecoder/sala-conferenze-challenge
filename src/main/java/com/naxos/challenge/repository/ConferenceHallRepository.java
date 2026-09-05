package com.naxos.challenge.repository;

import java.util.Optional;
import java.util.UUID;

import com.naxos.challenge.model.ConferenceHall;

/**
 * Non estende GenericRepository: la gestione CRUD completa delle conference hall
 * non è ancora stata implementata (nessun ConferenceHallService/Resource), questo
 * repository esiste solo perché BookingService deve risolvere la hall prenotata.
 */
public interface ConferenceHallRepository {

    Optional<ConferenceHall> findById(UUID id);

    ConferenceHall getConferenceHallById(UUID id);

    boolean conferenceHallAvailable(UUID conferenceHallId);
}
