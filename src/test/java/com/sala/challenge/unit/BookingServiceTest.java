package com.sala.challenge.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sala.challenge.dto.PagedResultDTO;
import com.sala.challenge.dto.booking.BaseDetailBookingDTO;
import com.sala.challenge.dto.booking.ConferenceHallReservationDTO;
import com.sala.challenge.dto.booking.CreateBookingDTO;
import com.sala.challenge.dto.booking.DetailBookingDTO;
import com.sala.challenge.dto.booking.InsertStatusDTO;
import com.sala.challenge.dto.booking.UpdateBookingDTO;
import com.sala.challenge.dto.search.BookingSearchRequest;
import com.sala.challenge.exception.ServiceException;
import com.sala.challenge.model.Booking;
import com.sala.challenge.model.Building;
import com.sala.challenge.model.ConferenceHall;
import com.sala.challenge.model.User;
import com.sala.challenge.model.enumerator.Role;
import com.sala.challenge.repository.BookingRepository;
import com.sala.challenge.security.JwtInspector;
import com.sala.challenge.services.BookingService;
import com.sala.challenge.services.ConferenceHallService;
import com.sala.challenge.services.UserService;
import com.sala.challenge.services.record.BookingInsertStatus;
import com.sala.challenge.util.DateTimeUtils;
import com.sala.challenge.util.PricingUtils;

import io.ebean.Database;
import io.ebean.PagedList;
import io.ebean.Transaction;
import jakarta.ws.rs.ForbiddenException;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    BookingRepository bookingRepository;

    @Mock
    ConferenceHallService conferenceHallService;

    @Mock
    UserService userService;

    @Mock
    Database database;

    @Mock
    JwtInspector jwtInspector;

    @Mock
    Transaction transaction;

    @InjectMocks
    BookingService bookingService;

    private MockedStatic<PricingUtils> pricingUtilsMock;
    private MockedStatic<DateTimeUtils> dateTimeUtilsMock;
    private MockedStatic<ServiceException> serviceExceptionMock;

    @BeforeEach
    void openStaticMocks() {
        pricingUtilsMock = mockStatic(PricingUtils.class);
        dateTimeUtilsMock = mockStatic(DateTimeUtils.class);
        serviceExceptionMock = mockStatic(ServiceException.class);
    }

    @AfterEach
    void closeStaticMocks() {
        pricingUtilsMock.close();
        dateTimeUtilsMock.close();
        serviceExceptionMock.close();
    }

    private Building building() {
        Building building = new Building();
        building.setId(UUID.randomUUID());
        return building;
    }

    private ConferenceHall conferenceHall(BigDecimal pricePerHour, boolean enabled) {
        ConferenceHall conferenceHall = new ConferenceHall();
        conferenceHall.setId(UUID.randomUUID());
        conferenceHall.setPricePerHour(pricePerHour);
        conferenceHall.setEnabled(enabled);
        conferenceHall.setBuilding(building());
        return conferenceHall;
    }

    private User user() {
        User user = new User();
        user.setId(UUID.randomUUID());
        return user;
    }

    private Booking booking(ConferenceHall conferenceHall, User user, LocalDateTime start, LocalDateTime end, boolean paid) {
        Booking booking = new Booking();
        booking.setId(UUID.randomUUID());
        booking.setConferenceHall(conferenceHall);
        booking.setUser(user);
        booking.setStartDateTime(start);
        booking.setEndDateTime(end);
        booking.setTotalCost(BigDecimal.TEN);
        booking.setPaid(paid);
        return booking;
    }

    private ConferenceHallReservationDTO reservation(UUID conferenceHallId, LocalDateTime start, LocalDateTime end) {
        return new ConferenceHallReservationDTO(conferenceHallId, start, end);
    }

    private CreateBookingDTO createBookingDto(ConferenceHallReservationDTO... reservations) {
        CreateBookingDTO dto = new CreateBookingDTO();
        dto.setConferenceHallReservationDTOs(List.of(reservations));
        return dto;
    }

    // ---- createBookings ----

    @Test
    void createBookings_bySubject_delegatesToSubjectUser() {
        User user = user();
        when(userService.getUserBySubject()).thenReturn(user);
        ConferenceHall hall = conferenceHall(BigDecimal.TEN, true);
        LocalDateTime start = LocalDateTime.now().plusDays(1);
        LocalDateTime end = start.plusHours(1);
        when(conferenceHallService.conferenceHallAvailable(hall.getId())).thenReturn(true);
        when(bookingRepository.existsOverlapping(eq(hall.getId()), any(), any(), any())).thenReturn(false);
        when(conferenceHallService.getConferenceHallById(hall.getId())).thenReturn(hall);
        when(database.beginTransaction()).thenReturn(transaction);
        pricingUtilsMock.when(() -> PricingUtils.computeTotalCost(any(BigDecimal.class), any(), any())).thenReturn(BigDecimal.TEN);

        bookingService.createBookings(createBookingDto(reservation(hall.getId(), start, end)));

        verify(userService).getUserBySubject();
    }

    @Test
    void createBookings_byAdminForUser_delegatesToSpecifiedUser() {
        UUID targetUserId = UUID.randomUUID();
        User user = user();
        when(userService.getUserEntityById(targetUserId)).thenReturn(user);
        ConferenceHall hall = conferenceHall(BigDecimal.TEN, true);
        LocalDateTime start = LocalDateTime.now().plusDays(1);
        LocalDateTime end = start.plusHours(1);
        when(conferenceHallService.conferenceHallAvailable(hall.getId())).thenReturn(true);
        when(bookingRepository.existsOverlapping(eq(hall.getId()), any(), any(), any())).thenReturn(false);
        when(conferenceHallService.getConferenceHallById(hall.getId())).thenReturn(hall);
        when(database.beginTransaction()).thenReturn(transaction);
        pricingUtilsMock.when(() -> PricingUtils.computeTotalCost(any(BigDecimal.class), any(), any())).thenReturn(BigDecimal.TEN);

        bookingService.createBookings(targetUserId, createBookingDto(reservation(hall.getId(), start, end)));

        verify(userService).getUserEntityById(targetUserId);
    }

    @Test
    void createBookings_hallNotAvailable_returnsFailureStatusForThatReservation() {
        when(userService.getUserBySubject()).thenReturn(user());
        UUID hallId = UUID.randomUUID();
        LocalDateTime start = LocalDateTime.now().plusDays(1);
        LocalDateTime end = start.plusHours(1);
        when(conferenceHallService.conferenceHallAvailable(hallId)).thenReturn(false);

        var result = bookingService.createBookings(createBookingDto(reservation(hallId, start, end)));

        assertThat(result.getBookingInsertStatuses()).containsExactly(new BookingInsertStatus(hallId, "Conference hall is not available"));
    }

    @Test
    void createBookings_overlapExistsApplicationCheck_returnsFailureStatus() {
        when(userService.getUserBySubject()).thenReturn(user());
        UUID hallId = UUID.randomUUID();
        LocalDateTime start = LocalDateTime.now().plusDays(1);
        LocalDateTime end = start.plusHours(1);
        when(conferenceHallService.conferenceHallAvailable(hallId)).thenReturn(true);
        when(bookingRepository.existsOverlapping(eq(hallId), any(), any(), any())).thenReturn(true);

        var result = bookingService.createBookings(createBookingDto(reservation(hallId, start, end)));

        assertThat(result.getBookingInsertStatuses()).containsExactly(new BookingInsertStatus(hallId, "Conference hall already booked for the selected time range"));
    }

    @Test
    void createBookings_overlapDetectedAtDbLevel_returnsFailureStatus() {
        when(userService.getUserBySubject()).thenReturn(user());
        ConferenceHall hall = conferenceHall(BigDecimal.TEN, true);
        LocalDateTime start = LocalDateTime.now().plusDays(1);
        LocalDateTime end = start.plusHours(1);
        when(conferenceHallService.conferenceHallAvailable(hall.getId())).thenReturn(true);
        when(bookingRepository.existsOverlapping(eq(hall.getId()), any(), any(), any())).thenReturn(false);
        when(conferenceHallService.getConferenceHallById(hall.getId())).thenReturn(hall);
        when(database.beginTransaction()).thenReturn(transaction);
        pricingUtilsMock.when(() -> PricingUtils.computeTotalCost(any(BigDecimal.class), any(), any())).thenReturn(BigDecimal.TEN);
        doThrow(new RuntimeException("conflict")).when(bookingRepository).save(any(), eq(transaction));
        serviceExceptionMock.when(() -> ServiceException.isOverlapViolation(any())).thenReturn(true);

        var result = bookingService.createBookings(createBookingDto(reservation(hall.getId(), start, end)));

        assertThat(result.getBookingInsertStatuses()).containsExactly(new BookingInsertStatus(hall.getId(), "Conference hall already booked for the selected time range"));
    }

    @Test
    void createBookings_unexpectedDbError_reportsFailureInsteadOfThrowing() {
        when(userService.getUserBySubject()).thenReturn(user());
        ConferenceHall hall = conferenceHall(BigDecimal.TEN, true);
        LocalDateTime start = LocalDateTime.now().plusDays(1);
        LocalDateTime end = start.plusHours(1);
        when(conferenceHallService.conferenceHallAvailable(hall.getId())).thenReturn(true);
        when(bookingRepository.existsOverlapping(eq(hall.getId()), any(), any(), any())).thenReturn(false);
        when(conferenceHallService.getConferenceHallById(hall.getId())).thenReturn(hall);
        when(database.beginTransaction()).thenReturn(transaction);
        pricingUtilsMock.when(() -> PricingUtils.computeTotalCost(any(BigDecimal.class), any(), any())).thenReturn(BigDecimal.TEN);
        doThrow(new RuntimeException("boom")).when(bookingRepository).save(any(), eq(transaction));
        serviceExceptionMock.when(() -> ServiceException.isOverlapViolation(any())).thenReturn(false);
        CreateBookingDTO dto = createBookingDto(reservation(hall.getId(), start, end));

        InsertStatusDTO result = bookingService.createBookings(dto);

        assertThat(result.getInsertFailureCount()).isEqualTo(1);
    }

    @Test
    void createBookings_successfulCreation_setsComputedTotalCost() {
        when(userService.getUserBySubject()).thenReturn(user());
        ConferenceHall hall = conferenceHall(BigDecimal.TEN, true);
        LocalDateTime start = LocalDateTime.now().plusDays(1);
        LocalDateTime end = start.plusHours(1);
        when(conferenceHallService.conferenceHallAvailable(hall.getId())).thenReturn(true);
        when(bookingRepository.existsOverlapping(eq(hall.getId()), any(), any(), any())).thenReturn(false);
        when(conferenceHallService.getConferenceHallById(hall.getId())).thenReturn(hall);
        when(database.beginTransaction()).thenReturn(transaction);
        pricingUtilsMock.when(() -> PricingUtils.computeTotalCost(any(BigDecimal.class), any(), any())).thenReturn(new BigDecimal("42.00"));
        ArgumentCaptor<Booking> savedBooking = ArgumentCaptor.forClass(Booking.class);

        bookingService.createBookings(createBookingDto(reservation(hall.getId(), start, end)));
        verify(bookingRepository).save(savedBooking.capture(), eq(transaction));

        assertThat(savedBooking.getValue().getTotalCost()).isEqualByComparingTo("42.00");
    }

    @Test
    void createBookings_mixedOutcomes_aggregatesCountsCorrectly() {
        when(userService.getUserBySubject()).thenReturn(user());
        ConferenceHall availableHall = conferenceHall(BigDecimal.TEN, true);
        UUID unavailableHallId = UUID.randomUUID();
        LocalDateTime start = LocalDateTime.now().plusDays(1);
        LocalDateTime end = start.plusHours(1);
        when(conferenceHallService.conferenceHallAvailable(availableHall.getId())).thenReturn(true);
        when(bookingRepository.existsOverlapping(eq(availableHall.getId()), any(), any(), any())).thenReturn(false);
        when(conferenceHallService.getConferenceHallById(availableHall.getId())).thenReturn(availableHall);
        when(database.beginTransaction()).thenReturn(transaction);
        pricingUtilsMock.when(() -> PricingUtils.computeTotalCost(any(BigDecimal.class), any(), any())).thenReturn(BigDecimal.TEN);
        when(conferenceHallService.conferenceHallAvailable(unavailableHallId)).thenReturn(false);
        CreateBookingDTO dto = createBookingDto(
                reservation(availableHall.getId(), start, end),
                reservation(unavailableHallId, start, end)
        );

        var result = bookingService.createBookings(dto);

        assertThat(result.getInsertSuccessCount()).isEqualTo(1);
    }

    // ---- updateBooking(UUID, UpdateBookingDTO) ----

    private UpdateBookingDTO updateDto(LocalDateTime start, LocalDateTime end) {
        UpdateBookingDTO dto = new UpdateBookingDTO();
        dto.setStartDateTime(start);
        dto.setEndDateTime(end);
        return dto;
    }

    @Test
    void updateBooking_hallNotAvailable_returnsFailureStatus() {
        ConferenceHall hall = conferenceHall(BigDecimal.TEN, true);
        Booking existing = booking(hall, user(), LocalDateTime.now().plusDays(2), LocalDateTime.now().plusDays(2).plusHours(1), false);
        when(bookingRepository.getBookingById(existing.getId())).thenReturn(existing);
        when(conferenceHallService.conferenceHallAvailable(hall.getId())).thenReturn(false);
        LocalDateTime newStart = LocalDateTime.now().plusDays(3);
        LocalDateTime newEnd = newStart.plusHours(1);

        var result = bookingService.updateBooking(existing.getId(), updateDto(newStart, newEnd));

        assertThat(result).isEqualTo(new BookingInsertStatus(hall.getId(), "Conference hall is not available"));
    }

    @Test
    void updateBooking_customerBookingAlreadyPaid_returnsFailureStatus() {
        ConferenceHall hall = conferenceHall(BigDecimal.TEN, true);
        Booking existing = booking(hall, user(), LocalDateTime.now().plusDays(2), LocalDateTime.now().plusDays(2).plusHours(1), true);
        when(bookingRepository.getBookingById(existing.getId())).thenReturn(existing);
        when(conferenceHallService.conferenceHallAvailable(hall.getId())).thenReturn(true);
        when(bookingRepository.existsOverlapping(eq(hall.getId()), any(), any(), eq(existing.getId()))).thenReturn(false);
        when(jwtInspector.hasRole(Role.CUSTOMER)).thenReturn(true);
        LocalDateTime newStart = LocalDateTime.now().plusDays(3);
        LocalDateTime newEnd = newStart.plusHours(1);

        var result = bookingService.updateBooking(existing.getId(), updateDto(newStart, newEnd));

        assertThat(result).isEqualTo(new BookingInsertStatus(hall.getId(), "Booking already paid, cannot be modified"));
    }

    @Test
    void updateBooking_customerBookingLessThan24HoursAway_returnsFailureStatus() {
        ConferenceHall hall = conferenceHall(BigDecimal.TEN, true);
        Booking existing = booking(hall, user(), LocalDateTime.now().plusDays(2), LocalDateTime.now().plusDays(2).plusHours(1), false);
        when(bookingRepository.getBookingById(existing.getId())).thenReturn(existing);
        when(conferenceHallService.conferenceHallAvailable(hall.getId())).thenReturn(true);
        when(bookingRepository.existsOverlapping(eq(hall.getId()), any(), any(), eq(existing.getId()))).thenReturn(false);
        when(jwtInspector.hasRole(Role.CUSTOMER)).thenReturn(true);
        dateTimeUtilsMock.when(() -> DateTimeUtils.isMoreThan24HoursAway(existing.getStartDateTime())).thenReturn(false);
        LocalDateTime newStart = LocalDateTime.now().plusDays(3);
        LocalDateTime newEnd = newStart.plusHours(1);

        var result = bookingService.updateBooking(existing.getId(), updateDto(newStart, newEnd));

        assertThat(result).isEqualTo(new BookingInsertStatus(hall.getId(), "Booking starts in less than 24 hours, cannot be modified"));
    }

    @Test
    void updateBooking_customerValidReschedule_returnsNull() {
        ConferenceHall hall = conferenceHall(BigDecimal.TEN, true);
        Booking existing = booking(hall, user(), LocalDateTime.now().plusDays(2), LocalDateTime.now().plusDays(2).plusHours(1), false);
        when(bookingRepository.getBookingById(existing.getId())).thenReturn(existing);
        when(conferenceHallService.conferenceHallAvailable(hall.getId())).thenReturn(true);
        when(bookingRepository.existsOverlapping(eq(hall.getId()), any(), any(), eq(existing.getId()))).thenReturn(false);
        when(jwtInspector.hasRole(Role.CUSTOMER)).thenReturn(true);
        dateTimeUtilsMock.when(() -> DateTimeUtils.isMoreThan24HoursAway(existing.getStartDateTime())).thenReturn(true);
        when(database.beginTransaction()).thenReturn(transaction);
        pricingUtilsMock.when(() -> PricingUtils.computeTotalCost(any(BigDecimal.class), any(), any())).thenReturn(BigDecimal.TEN);
        LocalDateTime newStart = LocalDateTime.now().plusDays(3);
        LocalDateTime newEnd = newStart.plusHours(1);

        var result = bookingService.updateBooking(existing.getId(), updateDto(newStart, newEnd));

        assertThat(result).isNull();
    }

    @Test
    void updateBooking_customerValidReschedule_updatesTotalCostUsingPricingUtils() {
        ConferenceHall hall = conferenceHall(BigDecimal.TEN, true);
        Booking existing = booking(hall, user(), LocalDateTime.now().plusDays(2), LocalDateTime.now().plusDays(2).plusHours(1), false);
        when(bookingRepository.getBookingById(existing.getId())).thenReturn(existing);
        when(conferenceHallService.conferenceHallAvailable(hall.getId())).thenReturn(true);
        when(bookingRepository.existsOverlapping(eq(hall.getId()), any(), any(), eq(existing.getId()))).thenReturn(false);
        when(jwtInspector.hasRole(Role.CUSTOMER)).thenReturn(true);
        dateTimeUtilsMock.when(() -> DateTimeUtils.isMoreThan24HoursAway(existing.getStartDateTime())).thenReturn(true);
        when(database.beginTransaction()).thenReturn(transaction);
        pricingUtilsMock.when(() -> PricingUtils.computeTotalCost(any(BigDecimal.class), any(), any())).thenReturn(new BigDecimal("99.00"));
        LocalDateTime newStart = LocalDateTime.now().plusDays(3);
        LocalDateTime newEnd = newStart.plusHours(1);

        bookingService.updateBooking(existing.getId(), updateDto(newStart, newEnd));

        assertThat(existing.getTotalCost()).isEqualByComparingTo("99.00");
    }

    @Test
    void updateBooking_adminBypassesPaidCheck_updatesEvenIfAlreadyPaid() {
        ConferenceHall hall = conferenceHall(BigDecimal.TEN, true);
        Booking existing = booking(hall, user(), LocalDateTime.now().plusDays(2), LocalDateTime.now().plusDays(2).plusHours(1), true);
        when(bookingRepository.getBookingById(existing.getId())).thenReturn(existing);
        when(conferenceHallService.conferenceHallAvailable(hall.getId())).thenReturn(true);
        when(bookingRepository.existsOverlapping(eq(hall.getId()), any(), any(), eq(existing.getId()))).thenReturn(false);
        when(jwtInspector.hasRole(Role.CUSTOMER)).thenReturn(false);
        when(database.beginTransaction()).thenReturn(transaction);
        pricingUtilsMock.when(() -> PricingUtils.computeTotalCost(any(BigDecimal.class), any(), any())).thenReturn(BigDecimal.TEN);
        LocalDateTime newStart = LocalDateTime.now().plusDays(3);
        LocalDateTime newEnd = newStart.plusHours(1);

        var result = bookingService.updateBooking(existing.getId(), updateDto(newStart, newEnd));

        assertThat(result).isNull();
    }

    @Test
    void updateBooking_overlapDetectedAtDbLevel_returnsFailureStatus() {
        ConferenceHall hall = conferenceHall(BigDecimal.TEN, true);
        Booking existing = booking(hall, user(), LocalDateTime.now().plusDays(2), LocalDateTime.now().plusDays(2).plusHours(1), false);
        when(bookingRepository.getBookingById(existing.getId())).thenReturn(existing);
        when(conferenceHallService.conferenceHallAvailable(hall.getId())).thenReturn(true);
        when(bookingRepository.existsOverlapping(eq(hall.getId()), any(), any(), eq(existing.getId()))).thenReturn(false);
        when(jwtInspector.hasRole(Role.CUSTOMER)).thenReturn(true);
        dateTimeUtilsMock.when(() -> DateTimeUtils.isMoreThan24HoursAway(existing.getStartDateTime())).thenReturn(true);
        when(database.beginTransaction()).thenReturn(transaction);
        pricingUtilsMock.when(() -> PricingUtils.computeTotalCost(any(BigDecimal.class), any(), any())).thenReturn(BigDecimal.TEN);
        doThrow(new RuntimeException("conflict")).when(bookingRepository).update(any(), eq(transaction));
        serviceExceptionMock.when(() -> ServiceException.isOverlapViolation(any())).thenReturn(true);
        LocalDateTime newStart = LocalDateTime.now().plusDays(3);
        LocalDateTime newEnd = newStart.plusHours(1);

        var result = bookingService.updateBooking(existing.getId(), updateDto(newStart, newEnd));

        assertThat(result).isEqualTo(new BookingInsertStatus(hall.getId(), "Conference hall already booked for the selected time range"));
    }

    @Test
    void updateBooking_transactionFails_throwsServiceException() {
        ConferenceHall hall = conferenceHall(BigDecimal.TEN, true);
        Booking existing = booking(hall, user(), LocalDateTime.now().plusDays(2), LocalDateTime.now().plusDays(2).plusHours(1), false);
        when(bookingRepository.getBookingById(existing.getId())).thenReturn(existing);
        when(conferenceHallService.conferenceHallAvailable(hall.getId())).thenReturn(true);
        when(bookingRepository.existsOverlapping(eq(hall.getId()), any(), any(), eq(existing.getId()))).thenReturn(false);
        when(jwtInspector.hasRole(Role.CUSTOMER)).thenReturn(true);
        dateTimeUtilsMock.when(() -> DateTimeUtils.isMoreThan24HoursAway(existing.getStartDateTime())).thenReturn(true);
        when(database.beginTransaction()).thenReturn(transaction);
        pricingUtilsMock.when(() -> PricingUtils.computeTotalCost(any(BigDecimal.class), any(), any())).thenReturn(BigDecimal.TEN);
        doThrow(new RuntimeException("boom")).when(bookingRepository).update(any(), eq(transaction));
        serviceExceptionMock.when(() -> ServiceException.isOverlapViolation(any())).thenReturn(false);
        LocalDateTime newStart = LocalDateTime.now().plusDays(3);
        LocalDateTime newEnd = newStart.plusHours(1);
        UUID bookingId = existing.getId();
        UpdateBookingDTO dto = updateDto(newStart, newEnd);

        Throwable thrown = catchThrowable(() -> bookingService.updateBooking(bookingId, dto));

        assertThat(thrown).isInstanceOf(ServiceException.class);
    }

    // ---- updateBooking(UUID userId, UUID id, UpdateBookingDTO) ----

    @Test
    void updateBooking_callerOwnsBooking_updatesSuccessfully() {
        User owner = user();
        ConferenceHall hall = conferenceHall(BigDecimal.TEN, true);
        Booking existing = booking(hall, owner, LocalDateTime.now().plusDays(2), LocalDateTime.now().plusDays(2).plusHours(1), false);
        when(bookingRepository.getBookingById(existing.getId())).thenReturn(existing);
        when(conferenceHallService.conferenceHallAvailable(hall.getId())).thenReturn(true);
        when(bookingRepository.existsOverlapping(eq(hall.getId()), any(), any(), eq(existing.getId()))).thenReturn(false);
        when(jwtInspector.hasRole(Role.CUSTOMER)).thenReturn(true);
        dateTimeUtilsMock.when(() -> DateTimeUtils.isMoreThan24HoursAway(existing.getStartDateTime())).thenReturn(true);
        when(database.beginTransaction()).thenReturn(transaction);
        pricingUtilsMock.when(() -> PricingUtils.computeTotalCost(any(BigDecimal.class), any(), any())).thenReturn(BigDecimal.TEN);
        LocalDateTime newStart = LocalDateTime.now().plusDays(3);
        LocalDateTime newEnd = newStart.plusHours(1);

        var result = bookingService.updateBooking(owner.getId(), existing.getId(), updateDto(newStart, newEnd));

        assertThat(result).isNull();
    }

    @Test
    void updateBooking_callerDoesNotOwnBooking_throwsServiceException() {
        ConferenceHall hall = conferenceHall(BigDecimal.TEN, true);
        Booking existing = booking(hall, user(), LocalDateTime.now().plusDays(2), LocalDateTime.now().plusDays(2).plusHours(1), false);
        when(bookingRepository.getBookingById(existing.getId())).thenReturn(existing);
        UUID otherUserId = UUID.randomUUID();
        UUID bookingId = existing.getId();
        UpdateBookingDTO dto = updateDto(LocalDateTime.now().plusDays(3), LocalDateTime.now().plusDays(3).plusHours(1));

        Throwable thrown = catchThrowable(() -> bookingService.updateBooking(otherUserId, bookingId, dto));

        assertThat(thrown).isInstanceOf(ServiceException.class).hasMessage("Booking does not belong to the given user");
    }

    // ---- getBookingById ----

    @Test
    void getBookingById_validId_returnsBookingDetail() {
        ConferenceHall hall = conferenceHall(BigDecimal.TEN, true);
        Booking existing = booking(hall, user(), LocalDateTime.now().plusDays(2), LocalDateTime.now().plusDays(2).plusHours(1), false);
        when(bookingRepository.getBookingById(existing.getId())).thenReturn(existing);

        DetailBookingDTO result = bookingService.getBookingById(existing.getId());

        assertThat(result.getId()).isEqualTo(existing.getId());
    }

    @Test
    void getBookingById_accessDenied_throwsForbiddenException() {
        ConferenceHall hall = conferenceHall(BigDecimal.TEN, true);
        Booking existing = booking(hall, user(), LocalDateTime.now().plusDays(2), LocalDateTime.now().plusDays(2).plusHours(1), false);
        when(bookingRepository.getBookingById(existing.getId())).thenReturn(existing);
        doThrow(new ForbiddenException("not allowed")).when(jwtInspector).checkAccessAllowed(existing.getUser().getId());
        UUID bookingId = existing.getId();

        Throwable thrown = catchThrowable(() -> bookingService.getBookingById(bookingId));

        assertThat(thrown).isInstanceOf(ForbiddenException.class);
    }

    // ---- findAllBookings ----

    @Test
    void findAllBookings_adminRole_usesRequestedUserIdConstraint() {
        UUID requestedUserId = UUID.randomUUID();
        BookingSearchRequest request = new BookingSearchRequest();
        request.setUserId(requestedUserId);
        when(jwtInspector.hasRole(Role.ADMIN)).thenReturn(true);
        PagedList<Booking> pagedList = TestPagedLists.empty();
        when(bookingRepository.search(request, requestedUserId)).thenReturn(pagedList);

        bookingService.findAllBookings(request);

        verify(bookingRepository).search(request, requestedUserId);
    }

    @Test
    void findAllBookings_customerRole_constrainsToOwnSubject() {
        UUID subjectId = UUID.randomUUID();
        BookingSearchRequest request = new BookingSearchRequest();
        request.setUserId(UUID.randomUUID());
        when(jwtInspector.hasRole(Role.ADMIN)).thenReturn(false);
        when(jwtInspector.hasRole(Role.ORGANIZER)).thenReturn(false);
        when(jwtInspector.getSubject()).thenReturn(subjectId);
        PagedList<Booking> pagedList = TestPagedLists.empty();
        when(bookingRepository.search(request, subjectId)).thenReturn(pagedList);

        bookingService.findAllBookings(request);

        verify(bookingRepository).search(request, subjectId);
    }

    // ---- findBookingsByUserId ----

    @Test
    void findBookingsByUserId_validUserId_returnsMappedList() {
        UUID userId = UUID.randomUUID();
        ConferenceHall hall = conferenceHall(BigDecimal.TEN, true);
        Booking first = booking(hall, user(), LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(1).plusHours(1), false);
        Booking second = booking(hall, user(), LocalDateTime.now().plusDays(2), LocalDateTime.now().plusDays(2).plusHours(1), false);
        when(bookingRepository.findByUserId(userId)).thenReturn(List.of(first, second));

        List<BaseDetailBookingDTO> result = bookingService.findBookingsByUserId(userId);

        assertThat(result).hasSize(2);
    }

    // ---- deleteBooking ----

    @Test
    void deleteBooking_validId_deletesBooking() {
        ConferenceHall hall = conferenceHall(BigDecimal.TEN, true);
        Booking existing = booking(hall, user(), LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(1).plusHours(1), false);
        when(bookingRepository.getBookingById(existing.getId())).thenReturn(existing);
        when(database.beginTransaction()).thenReturn(transaction);

        bookingService.deleteBooking(existing.getId());

        verify(bookingRepository).delete(existing.getId(), transaction);
    }

    @Test
    void deleteBooking_selfServiceOwnerMismatch_throwsServiceException() {
        ConferenceHall hall = conferenceHall(BigDecimal.TEN, true);
        Booking existing = booking(hall, user(), LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(1).plusHours(1), false);
        when(bookingRepository.getBookingById(existing.getId())).thenReturn(existing);
        UUID otherUserId = UUID.randomUUID();
        UUID bookingId = existing.getId();

        Throwable thrown = catchThrowable(() -> bookingService.deleteBooking(otherUserId, bookingId));

        assertThat(thrown).isInstanceOf(ServiceException.class).hasMessage("Booking does not belong to the given user");
    }

    @Test
    void deleteBooking_transactionFails_throwsServiceException() {
        ConferenceHall hall = conferenceHall(BigDecimal.TEN, true);
        Booking existing = booking(hall, user(), LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(1).plusHours(1), false);
        when(bookingRepository.getBookingById(existing.getId())).thenReturn(existing);
        when(database.beginTransaction()).thenReturn(transaction);
        doThrow(new RuntimeException("boom")).when(bookingRepository).delete(existing.getId(), transaction);
        UUID bookingId = existing.getId();

        Throwable thrown = catchThrowable(() -> bookingService.deleteBooking(bookingId));

        assertThat(thrown).isInstanceOf(ServiceException.class);
    }
}
