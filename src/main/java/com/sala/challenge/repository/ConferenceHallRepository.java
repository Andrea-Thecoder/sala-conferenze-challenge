package com.sala.challenge.repository;

import java.util.UUID;

import com.sala.challenge.dto.search.ConferenceHallSearchRequest;
import com.sala.challenge.model.ConferenceHall;
import io.ebean.PagedList;

public interface ConferenceHallRepository extends GenericRepository<ConferenceHall, UUID> {

    ConferenceHall getConferenceHallById(UUID id);

    boolean conferenceHallAvailable(UUID conferenceHallId);

    PagedList<ConferenceHall> search(ConferenceHallSearchRequest request);
}
