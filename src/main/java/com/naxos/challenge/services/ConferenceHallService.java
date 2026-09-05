package com.naxos.challenge.services;

import com.naxos.challenge.model.ConferenceHall;
import com.naxos.challenge.repository.ConferenceHallRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

@ApplicationScoped
@Slf4j
public class ConferenceHallService {

    @Inject
    ConferenceHallRepository conferenceHallRepository;

    public boolean conferenceHallAvailable(UUID conferenceHallId) {
        return conferenceHallRepository.conferenceHallAvailable(conferenceHallId);
    }

    public ConferenceHall getConferenceHallById(UUID conferenceHallId) {
        return conferenceHallRepository.getConferenceHallById(conferenceHallId);
    }
}
