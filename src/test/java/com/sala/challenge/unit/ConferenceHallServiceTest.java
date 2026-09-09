package com.sala.challenge.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
import com.sala.challenge.services.ConferenceHallService;

import io.ebean.Database;
import io.ebean.PagedList;
import io.ebean.Transaction;

@ExtendWith(MockitoExtension.class)
class ConferenceHallServiceTest {

    @Mock
    ConferenceHallRepository conferenceHallRepository;

    @Mock
    BuildingRepository buildingRepository;

    @Mock
    Database database;

    @Mock
    JwtInspector jwtInspector;

    @Mock
    Transaction transaction;

    @InjectMocks
    ConferenceHallService conferenceHallService;

    private ConferenceHall conferenceHall(boolean enabled) {
        ConferenceHall conferenceHall = new ConferenceHall();
        conferenceHall.setId(UUID.randomUUID());
        conferenceHall.setPricePerHour(BigDecimal.TEN);
        conferenceHall.setEnabled(enabled);
        Building building = new Building();
        building.setId(UUID.randomUUID());
        conferenceHall.setBuilding(building);
        return conferenceHall;
    }

    private CreateConferenceHallDTO createDto(UUID buildingId) {
        CreateConferenceHallDTO dto = new CreateConferenceHallDTO();
        dto.setName("Sala Aurora");
        dto.setSize(10);
        dto.setPricePerHour(BigDecimal.TEN);
        dto.setBuildingId(buildingId);
        dto.setFloor(1);
        dto.setRoomNumber("101");
        return dto;
    }

    private UpdateConferenceHallDTO updateDto() {
        UpdateConferenceHallDTO dto = new UpdateConferenceHallDTO();
        dto.setName("Sala Rinnovata");
        dto.setSize(20);
        dto.setPricePerHour(new BigDecimal("99.00"));
        dto.setFloor(2);
        dto.setRoomNumber("202");
        return dto;
    }

    // ---- conferenceHallAvailable ----

    @Test
    void conferenceHallAvailable_delegatesToRepository_returnsRepositoryResult() {
        UUID hallId = UUID.randomUUID();
        when(conferenceHallRepository.conferenceHallAvailable(hallId)).thenReturn(true);

        boolean result = conferenceHallService.conferenceHallAvailable(hallId);

        assertThat(result).isTrue();
    }

    // ---- getConferenceHallById ----

    @Test
    void getConferenceHallById_delegatesToRepository_returnsRepositoryResult() {
        ConferenceHall hall = conferenceHall(true);
        when(conferenceHallRepository.getConferenceHallById(hall.getId())).thenReturn(hall);

        ConferenceHall result = conferenceHallService.getConferenceHallById(hall.getId());

        assertThat(result).isEqualTo(hall);
    }

    // ---- createConferenceHall ----

    @Test
    void createConferenceHall_validDto_returnsGeneratedHallId() {
        Building building = new Building();
        building.setId(UUID.randomUUID());
        UUID generatedId = UUID.randomUUID();
        when(buildingRepository.getBuildingById(building.getId())).thenReturn(building);
        when(database.beginTransaction()).thenReturn(transaction);
        doAnswer(invocation -> {
            ConferenceHall saved = invocation.getArgument(0);
            saved.setId(generatedId);
            return null;
        }).when(conferenceHallRepository).save(any(), eq(transaction));

        UUID result = conferenceHallService.createConferenceHall(createDto(building.getId()));

        assertThat(result).isEqualTo(generatedId);
    }

    @Test
    void createConferenceHall_transactionFails_throwsServiceException() {
        Building building = new Building();
        building.setId(UUID.randomUUID());
        when(buildingRepository.getBuildingById(building.getId())).thenReturn(building);
        when(database.beginTransaction()).thenReturn(transaction);
        doThrow(new RuntimeException("boom")).when(conferenceHallRepository).save(any(), eq(transaction));
        CreateConferenceHallDTO dto = createDto(building.getId());

        Throwable thrown = catchThrowable(() -> conferenceHallService.createConferenceHall(dto));

        assertThat(thrown).isInstanceOf(ServiceException.class);
    }

    // ---- getConferenceHallDetailById ----

    @Test
    void getConferenceHallDetailById_hallEnabled_returnsDetail() {
        ConferenceHall hall = conferenceHall(true);
        when(conferenceHallRepository.getConferenceHallById(hall.getId())).thenReturn(hall);

        DetailConferenceHallDTO result = conferenceHallService.getConferenceHallDetailById(hall.getId());

        assertThat(result.getId()).isEqualTo(hall.getId());
    }

    @Test
    void getConferenceHallDetailById_hallDisabledAndCallerIsAdmin_returnsDetail() {
        ConferenceHall hall = conferenceHall(false);
        when(conferenceHallRepository.getConferenceHallById(hall.getId())).thenReturn(hall);
        when(jwtInspector.hasRole(Role.ADMIN)).thenReturn(true);

        DetailConferenceHallDTO result = conferenceHallService.getConferenceHallDetailById(hall.getId());

        assertThat(result.getId()).isEqualTo(hall.getId());
    }

    @Test
    void getConferenceHallDetailById_hallDisabledAndCallerIsOrganizer_returnsDetail() {
        ConferenceHall hall = conferenceHall(false);
        when(conferenceHallRepository.getConferenceHallById(hall.getId())).thenReturn(hall);
        when(jwtInspector.hasRole(Role.ADMIN)).thenReturn(false);
        when(jwtInspector.hasRole(Role.ORGANIZER)).thenReturn(true);

        DetailConferenceHallDTO result = conferenceHallService.getConferenceHallDetailById(hall.getId());

        assertThat(result.getId()).isEqualTo(hall.getId());
    }

    @Test
    void getConferenceHallDetailById_hallDisabledAndCallerIsCustomer_throwsServiceExceptionAsIfNotFound() {
        ConferenceHall hall = conferenceHall(false);
        when(conferenceHallRepository.getConferenceHallById(hall.getId())).thenReturn(hall);
        when(jwtInspector.hasRole(Role.ADMIN)).thenReturn(false);
        when(jwtInspector.hasRole(Role.ORGANIZER)).thenReturn(false);
        UUID hallId = hall.getId();

        Throwable thrown = catchThrowable(() -> conferenceHallService.getConferenceHallDetailById(hallId));

        assertThat(thrown).isInstanceOf(ServiceException.class).hasMessage("Conference hall not found");
    }

    // ---- findAllConferenceHalls ----

    @Test
    void findAllConferenceHalls_adminRole_doesNotForceEnabledFilter() {
        ConferenceHallSearchRequest request = new ConferenceHallSearchRequest();
        when(jwtInspector.hasRole(Role.ADMIN)).thenReturn(true);
        PagedList<ConferenceHall> pagedList = TestPagedLists.empty();
        when(conferenceHallRepository.search(request)).thenReturn(pagedList);

        conferenceHallService.findAllConferenceHalls(request);

        assertThat(request.getEnabled()).isNull();
    }

    @Test
    void findAllConferenceHalls_customerRole_forcesEnabledFilterTrue() {
        ConferenceHallSearchRequest request = new ConferenceHallSearchRequest();
        when(jwtInspector.hasRole(Role.ADMIN)).thenReturn(false);
        when(jwtInspector.hasRole(Role.ORGANIZER)).thenReturn(false);
        PagedList<ConferenceHall> pagedList = TestPagedLists.empty();
        when(conferenceHallRepository.search(request)).thenReturn(pagedList);

        conferenceHallService.findAllConferenceHalls(request);

        assertThat(request.getEnabled()).isTrue();
    }

    // ---- updateConferenceHall ----

    @Test
    void updateConferenceHall_validRequest_updatesHallName() {
        ConferenceHall hall = conferenceHall(true);
        when(conferenceHallRepository.getConferenceHallById(hall.getId())).thenReturn(hall);
        when(database.beginTransaction()).thenReturn(transaction);

        conferenceHallService.updateConferenceHall(hall.getId(), updateDto());

        assertThat(hall.getName()).isEqualTo("Sala Rinnovata");
    }

    @Test
    void updateConferenceHall_transactionFails_throwsServiceException() {
        ConferenceHall hall = conferenceHall(true);
        when(conferenceHallRepository.getConferenceHallById(hall.getId())).thenReturn(hall);
        when(database.beginTransaction()).thenReturn(transaction);
        doThrow(new RuntimeException("boom")).when(conferenceHallRepository).update(any(), eq(transaction));
        UUID hallId = hall.getId();
        UpdateConferenceHallDTO dto = updateDto();

        Throwable thrown = catchThrowable(() -> conferenceHallService.updateConferenceHall(hallId, dto));

        assertThat(thrown).isInstanceOf(ServiceException.class);
    }

    // ---- disableConferenceHall ----

    @Test
    void disableConferenceHall_validId_setsEnabledFalse() {
        ConferenceHall hall = conferenceHall(true);
        when(conferenceHallRepository.getConferenceHallById(hall.getId())).thenReturn(hall);
        when(database.beginTransaction()).thenReturn(transaction);

        conferenceHallService.disableConferenceHall(hall.getId());

        assertThat(hall.isEnabled()).isFalse();
    }

    @Test
    void disableConferenceHall_transactionFails_throwsServiceException() {
        ConferenceHall hall = conferenceHall(true);
        when(conferenceHallRepository.getConferenceHallById(hall.getId())).thenReturn(hall);
        when(database.beginTransaction()).thenReturn(transaction);
        doThrow(new RuntimeException("boom")).when(conferenceHallRepository).update(any(), eq(transaction));
        UUID hallId = hall.getId();

        Throwable thrown = catchThrowable(() -> conferenceHallService.disableConferenceHall(hallId));

        assertThat(thrown).isInstanceOf(ServiceException.class);
    }

    // ---- deleteConferenceHall ----

    @Test
    void deleteConferenceHall_validId_deletesHall() {
        UUID hallId = UUID.randomUUID();
        when(database.beginTransaction()).thenReturn(transaction);

        conferenceHallService.deleteConferenceHall(hallId);

        verify(conferenceHallRepository).delete(hallId, transaction);
    }

    @Test
    void deleteConferenceHall_transactionFails_throwsServiceException() {
        UUID hallId = UUID.randomUUID();
        when(database.beginTransaction()).thenReturn(transaction);
        doThrow(new RuntimeException("boom")).when(conferenceHallRepository).delete(hallId, transaction);

        Throwable thrown = catchThrowable(() -> conferenceHallService.deleteConferenceHall(hallId));

        assertThat(thrown).isInstanceOf(ServiceException.class);
    }
}
