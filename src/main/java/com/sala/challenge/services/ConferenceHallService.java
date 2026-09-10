package com.sala.challenge.services;

import java.util.UUID;

import com.sala.challenge.dto.PagedResultDTO;
import com.sala.challenge.dto.conference.BaseDetailConferenceHallDTO;
import com.sala.challenge.dto.conference.CreateConferenceHallDTO;
import com.sala.challenge.dto.conference.DetailConferenceHallDTO;
import com.sala.challenge.dto.conference.UpdateConferenceHallDTO;
import com.sala.challenge.dto.search.ConferenceHallSearchRequest;
import com.sala.challenge.exception.ServiceException;
import com.sala.challenge.model.Building;
import com.sala.challenge.model.ConferenceHall;
import com.sala.challenge.model.enumerator.Role;
import com.sala.challenge.repository.BuildingRepository;
import com.sala.challenge.repository.ConferenceHallRepository;
import com.sala.challenge.security.JwtInspector;
import io.ebean.Database;
import io.ebean.PagedList;
import io.ebean.Transaction;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@ApplicationScoped
@Slf4j
public class ConferenceHallService {

    @Inject
    ConferenceHallRepository conferenceHallRepository;

    @Inject
    BuildingRepository buildingRepository;

    @Inject
    Database database;

    @Inject
    JwtInspector jwtInspector;

    public boolean conferenceHallAvailable(UUID conferenceHallId) {
        return conferenceHallRepository.conferenceHallAvailable(conferenceHallId);
    }

    public ConferenceHall getConferenceHallById(UUID conferenceHallId) {
        return conferenceHallRepository.getConferenceHallById(conferenceHallId);
    }

    public UUID createConferenceHall(CreateConferenceHallDTO dto) {
        log.info("ConferenceHallService - createConferenceHall : Creating new conference hall");
        Building building = buildingRepository.getBuildingById(dto.getBuildingId());
        try (Transaction tx = database.beginTransaction()) {
            ConferenceHall conferenceHall = dto.toEntity(building);
            conferenceHallRepository.save(conferenceHall, tx);
            tx.commit();
            log.info("ConferenceHallService - createConferenceHall : Created conference hall {}", conferenceHall.getId());
            return conferenceHall.getId();
        } catch (Exception e) {
            log.error("ConferenceHallService - createConferenceHall : Error creating conference hall", e);
            throw new ServiceException("Error while creating conference hall. Try again later.");
        }
    }

    /**
     * Stesso comportamento di "non trovato" del repository (ServiceException, non
     * NotFoundException) per una sala disabilitata vista da un CUSTOMER: se un caso
     * restituisse 404 e l'altro 400, lo status code diverso diventerebbe di per sé
     * un modo per distinguere "non esiste" da "esiste ma è disabilitata" — esattamente
     * l'enumerazione che questo controllo deve evitare.
     */
    public DetailConferenceHallDTO getConferenceHallDetailById(UUID id) {
        ConferenceHall conferenceHall = conferenceHallRepository.getConferenceHallById(id);
        if (!conferenceHall.isEnabled() && !jwtInspector.hasRole(Role.ADMIN) && !jwtInspector.hasRole(Role.ORGANIZER)) {
            log.error("ConferenceHallService - getConferenceHallDetailById : Conference hall {} is disabled, hidden from non-admin/organizer caller", id);
            throw new ServiceException("Conference hall not found");
        }
        return DetailConferenceHallDTO.of(conferenceHall);
    }

    public PagedResultDTO<BaseDetailConferenceHallDTO> findAllConferenceHalls(ConferenceHallSearchRequest request) {
        log.info("ConferenceHallService - findAllConferenceHalls : Searching conference halls, page {} size {}", request.getPage(), request.getSize());
        if (!jwtInspector.hasRole(Role.ADMIN) && !jwtInspector.hasRole(Role.ORGANIZER)) {
            request.setEnabled(true);
        }
        PagedList<ConferenceHall> pagedList = conferenceHallRepository.search(request);
        return PagedResultDTO.of(pagedList, BaseDetailConferenceHallDTO::of);
    }

    public void updateConferenceHall(UUID id, UpdateConferenceHallDTO dto) {
        log.info("ConferenceHallService - updateConferenceHall : Updating conference hall {}", id);
        try (Transaction tx = database.beginTransaction()) {
            ConferenceHall conferenceHall = conferenceHallRepository.getConferenceHallById(id);
            dto.toUpdate(conferenceHall);
            conferenceHallRepository.update(conferenceHall, tx);
            tx.commit();
        } catch (Exception e) {
            log.error("ConferenceHallService - updateConferenceHall : Error updating conference hall {}", id, e);
            throw new ServiceException("Error while updating conference hall. Try again later.");
        }
        log.info("ConferenceHallService - updateConferenceHall : Updated conference hall {}", id);
    }

    /**
     * Soft delete: disabilita la sala (enabled=false) invece di cancellarla. Azione
     * di gestione ordinaria — ADMIN e ORGANIZER possono entrambi disabilitare una
     * sala (es. fuori servizio temporaneamente), a differenza della hard delete.
     */
    public void disableConferenceHall(UUID id) {
        log.info("ConferenceHallService - disableConferenceHall : Disabling conference hall {}", id);
        try (Transaction tx = database.beginTransaction()) {
            ConferenceHall conferenceHall = conferenceHallRepository.getConferenceHallById(id);
            conferenceHall.setEnabled(false);
            conferenceHallRepository.update(conferenceHall, tx);
            tx.commit();
        } catch (Exception e) {
            log.error("ConferenceHallService - disableConferenceHall : Error disabling conference hall {}", id, e);
            throw new ServiceException("Error while disabling conference hall. Try again later.");
        }
        log.info("ConferenceHallService - disableConferenceHall : Disabled conference hall {}", id);
    }

    /**
     * Hard delete: cancellazione fisica, irreversibile — solo ADMIN (enforced anche
     * a livello di Resource via @RolesAllowed), a differenza della soft delete che
     * un ORGANIZER può eseguire.
     */
    public void deleteConferenceHall(UUID id) {
        log.info("ConferenceHallService - deleteConferenceHall : Deleting conference hall {}", id);
        try (Transaction tx = database.beginTransaction()) {
            conferenceHallRepository.delete(id, tx);
            tx.commit();
        } catch (Exception e) {
            log.error("ConferenceHallService - deleteConferenceHall : Error deleting conference hall {}", id, e);
            throw new ServiceException("Error while deleting conference hall. Try again later.");
        }
    }
}
