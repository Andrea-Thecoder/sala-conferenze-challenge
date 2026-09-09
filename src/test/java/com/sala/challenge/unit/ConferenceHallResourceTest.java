package com.sala.challenge.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sala.challenge.api.ConferenceHallResource;
import com.sala.challenge.dto.PagedResultDTO;
import com.sala.challenge.dto.SimpleResultDTO;
import com.sala.challenge.dto.conference.BaseDetailConferenceHallDTO;
import com.sala.challenge.dto.conference.CreateConferenceHallDTO;
import com.sala.challenge.dto.conference.DetailConferenceHallDTO;
import com.sala.challenge.dto.conference.UpdateConferenceHallDTO;
import com.sala.challenge.dto.search.ConferenceHallSearchRequest;
import com.sala.challenge.services.ConferenceHallService;

@ExtendWith(MockitoExtension.class)
class ConferenceHallResourceTest {

    @Mock
    ConferenceHallService conferenceHallService;

    @InjectMocks
    ConferenceHallResource conferenceHallResource;

    @Test
    void createConferenceHall_validDto_returnsCreatedIdInPayload() {
        CreateConferenceHallDTO dto = new CreateConferenceHallDTO();
        UUID createdId = UUID.randomUUID();
        when(conferenceHallService.createConferenceHall(dto)).thenReturn(createdId);

        SimpleResultDTO<UUID> result = conferenceHallResource.createConferenceHall(dto);

        assertThat(result.getPayload()).isEqualTo(createdId);
    }

    @Test
    void getConferenceHallById_validId_returnsServiceResult() {
        UUID hallId = UUID.randomUUID();
        DetailConferenceHallDTO detail = new DetailConferenceHallDTO();
        when(conferenceHallService.getConferenceHallDetailById(hallId)).thenReturn(detail);

        DetailConferenceHallDTO result = conferenceHallResource.getConferenceHallById(hallId);

        assertThat(result).isEqualTo(detail);
    }

    @Test
    void findAllConferenceHalls_delegatesToServiceWithRequest() {
        ConferenceHallSearchRequest request = new ConferenceHallSearchRequest();
        PagedResultDTO<BaseDetailConferenceHallDTO> pagedResult = new PagedResultDTO<>();
        when(conferenceHallService.findAllConferenceHalls(request)).thenReturn(pagedResult);

        PagedResultDTO<BaseDetailConferenceHallDTO> result = conferenceHallResource.findAllConferenceHalls(request);

        assertThat(result).isEqualTo(pagedResult);
    }

    @Test
    void updateConferenceHall_validRequest_delegatesToService() {
        UUID hallId = UUID.randomUUID();
        UpdateConferenceHallDTO dto = new UpdateConferenceHallDTO();

        conferenceHallResource.updateConferenceHall(hallId, dto);

        verify(conferenceHallService).updateConferenceHall(hallId, dto);
    }

    @Test
    void disableConferenceHall_validId_delegatesToService() {
        UUID hallId = UUID.randomUUID();

        conferenceHallResource.disableConferenceHall(hallId);

        verify(conferenceHallService).disableConferenceHall(hallId);
    }

    @Test
    void deleteConferenceHall_validId_delegatesToService() {
        UUID hallId = UUID.randomUUID();

        conferenceHallResource.deleteConferenceHall(hallId);

        verify(conferenceHallService).deleteConferenceHall(hallId);
    }
}
