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

import com.sala.challenge.api.BookingResource;
import com.sala.challenge.dto.PagedResultDTO;
import com.sala.challenge.dto.SimpleResultDTO;
import com.sala.challenge.dto.booking.BaseDetailBookingDTO;
import com.sala.challenge.dto.booking.CreateBookingDTO;
import com.sala.challenge.dto.booking.DetailBookingDTO;
import com.sala.challenge.dto.booking.InsertStatusDTO;
import com.sala.challenge.dto.booking.UpdateBookingDTO;
import com.sala.challenge.dto.search.BookingSearchRequest;
import com.sala.challenge.services.BookingService;
import com.sala.challenge.services.record.BookingInsertStatus;

@ExtendWith(MockitoExtension.class)
class BookingResourceTest {

    @Mock
    BookingService bookingService;

    @InjectMocks
    BookingResource bookingResource;

    @Test
    void createBookings_validDto_wrapsServiceResultInPayload() {
        CreateBookingDTO dto = new CreateBookingDTO();
        InsertStatusDTO status = InsertStatusDTO.of(1, 1, 0, java.util.List.of());
        when(bookingService.createBookings(dto)).thenReturn(status);

        SimpleResultDTO<InsertStatusDTO> result = bookingResource.createBookings(dto);

        assertThat(result.getPayload()).isEqualTo(status);
    }

    @Test
    void createBookingsForUser_validRequest_delegatesToServiceForGivenUser() {
        UUID targetUserId = UUID.randomUUID();
        CreateBookingDTO dto = new CreateBookingDTO();
        InsertStatusDTO status = InsertStatusDTO.of(1, 1, 0, java.util.List.of());
        when(bookingService.createBookings(targetUserId, dto)).thenReturn(status);

        SimpleResultDTO<InsertStatusDTO> result = bookingResource.createBookingsForUser(targetUserId, dto);

        assertThat(result.getPayload()).isEqualTo(status);
    }

    @Test
    void getBookingById_validId_returnsServiceResult() {
        UUID bookingId = UUID.randomUUID();
        DetailBookingDTO detail = new DetailBookingDTO();
        when(bookingService.getBookingById(bookingId)).thenReturn(detail);

        DetailBookingDTO result = bookingResource.getBookingById(bookingId);

        assertThat(result).isEqualTo(detail);
    }

    @Test
    void findAllBookings_delegatesToServiceWithRequest() {
        BookingSearchRequest request = new BookingSearchRequest();
        PagedResultDTO<BaseDetailBookingDTO> pagedResult = new PagedResultDTO<>();
        when(bookingService.findAllBookings(request)).thenReturn(pagedResult);

        PagedResultDTO<BaseDetailBookingDTO> result = bookingResource.findAllBookings(request);

        assertThat(result).isEqualTo(pagedResult);
    }

    @Test
    void updateBooking_successfulReschedule_returnsUpdatedMessage() {
        UUID bookingId = UUID.randomUUID();
        UpdateBookingDTO dto = new UpdateBookingDTO();
        when(bookingService.updateBooking(bookingId, dto)).thenReturn(null);

        SimpleResultDTO<BookingInsertStatus> result = bookingResource.updateBooking(bookingId, dto);

        assertThat(result.getMessage()).isEqualTo("Booking updated");
    }

    @Test
    void updateBooking_rejectedReschedule_returnsNotUpdatedMessageWithPayload() {
        UUID bookingId = UUID.randomUUID();
        UpdateBookingDTO dto = new UpdateBookingDTO();
        BookingInsertStatus status = new BookingInsertStatus(UUID.randomUUID(), "Conference hall already booked for the selected time range");
        when(bookingService.updateBooking(bookingId, dto)).thenReturn(status);

        SimpleResultDTO<BookingInsertStatus> result = bookingResource.updateBooking(bookingId, dto);

        assertThat(result.getPayload()).isEqualTo(status);
    }

    @Test
    void updateBookingForUser_successfulReschedule_returnsUpdatedMessage() {
        UUID userId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();
        UpdateBookingDTO dto = new UpdateBookingDTO();
        when(bookingService.updateBooking(userId, bookingId, dto)).thenReturn(null);

        SimpleResultDTO<BookingInsertStatus> result = bookingResource.updateBookingForUser(userId, bookingId, dto);

        assertThat(result.getMessage()).isEqualTo("Booking updated");
    }

    @Test
    void updateBookingForUser_rejectedReschedule_returnsNotUpdatedMessage() {
        UUID userId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();
        UpdateBookingDTO dto = new UpdateBookingDTO();
        BookingInsertStatus status = new BookingInsertStatus(UUID.randomUUID(), "Booking already paid, cannot be modified");
        when(bookingService.updateBooking(userId, bookingId, dto)).thenReturn(status);

        SimpleResultDTO<BookingInsertStatus> result = bookingResource.updateBookingForUser(userId, bookingId, dto);

        assertThat(result.getMessage()).isEqualTo("Booking not updated");
    }

    @Test
    void deleteBooking_validId_delegatesToService() {
        UUID bookingId = UUID.randomUUID();

        bookingResource.deleteBooking(bookingId);

        verify(bookingService).deleteBooking(bookingId);
    }

    @Test
    void deleteBookingForUser_validRequest_delegatesToServiceForGivenUser() {
        UUID userId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();

        bookingResource.deleteBookingForUser(userId, bookingId);

        verify(bookingService).deleteBooking(userId, bookingId);
    }
}
