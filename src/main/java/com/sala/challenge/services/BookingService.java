package com.sala.challenge.services;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.sala.challenge.services.record.BookingInsertStatus;
import com.sala.challenge.dto.PagedResultDTO;
import com.sala.challenge.dto.booking.*;
import com.sala.challenge.dto.search.BookingSearchRequest;
import com.sala.challenge.exception.ServiceException;
import com.sala.challenge.model.Booking;
import com.sala.challenge.model.ConferenceHall;
import com.sala.challenge.model.User;
import com.sala.challenge.model.enumerator.Role;
import com.sala.challenge.repository.BookingRepository;
import com.sala.challenge.security.JwtInspector;
import com.sala.challenge.util.DateTimeUtils;
import com.sala.challenge.util.PricingUtils;
import io.ebean.Database;
import io.ebean.PagedList;
import io.ebean.Transaction;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@ApplicationScoped
@Slf4j
public class BookingService {

    @Inject
    BookingRepository bookingRepository;

    @Inject
    ConferenceHallService conferenceHallService;

    @Inject
    UserService userService;

    @Inject
    Database database;

    @Inject
    JwtInspector jwtInspector;

    public InsertStatusDTO createBookings(CreateBookingDTO dto) {
        return createBookings(userService.getUserBySubject(), dto);
    }

    public InsertStatusDTO createBookings(UUID userId, CreateBookingDTO dto) {
        return createBookings(userService.getUserEntityById(userId), dto);
    }

    private InsertStatusDTO createBookings(User user, CreateBookingDTO dto) {
        log.info("BookingService - createBooking : Creating new booking for hall for userID {}", user.getId());

        int totalInsert = dto.getConferenceHallReservationDTOs().size();
        int insertSuccessCount = 0;
        int insertFailureCount = 0;
        List<BookingInsertStatus> bookingsStatus = new ArrayList<>();

        for (ConferenceHallReservationDTO chrDTO : dto.getConferenceHallReservationDTOs()) {
            BookingInsertStatus status = createBooking(user, chrDTO);
            if (status != null) {
                bookingsStatus.add(status);
                insertFailureCount++;
            } else {
                insertSuccessCount++;
            }
        }
        return InsertStatusDTO.of(totalInsert, insertSuccessCount, insertFailureCount, bookingsStatus);
    }

    private BookingInsertStatus createBooking(User user, ConferenceHallReservationDTO chrDTO) {
        BookingInsertStatus status = checkBookingRequest(chrDTO.getConferenceHallId(), chrDTO.getStartDateTime(), chrDTO.getEndDateTime());
        if (status != null) {
            return status;
        }
        try (Transaction tx = database.beginTransaction()) {
            ConferenceHall conferenceHall = conferenceHallService.getConferenceHallById(chrDTO.getConferenceHallId());
            Booking booking = chrDTO.toEntity(conferenceHall, user);
            booking.setTotalCost(
                    PricingUtils.computeTotalCost(conferenceHall.getPricePerHour(),
                            chrDTO.getStartDateTime(),
                            chrDTO.getEndDateTime())
            );
            bookingRepository.save(booking, tx);
            tx.commit();
            return null;
        } catch (Exception e) {
            if (ServiceException.isOverlapViolation(e)) {
                log.warn("BookingService - createBooking : Overlap detected at database level for hall {}", chrDTO.getConferenceHallId());
                return new BookingInsertStatus(chrDTO.getConferenceHallId(), "Conference hall already booked for the selected time range");
            }
            // Non rilanciare: createBookings() chiama questo metodo dentro un loop, una
            // transazione per prenotazione richiesta. Le prenotazioni precedenti del
            // batch sono già committate — un throw qui le nasconderebbe al client
            // (richiesta fallita in blocco) pur restando salvate nel DB. Si logga
            // l'errore e si riporta il fallimento nello status della singola voce,
            // così il batch prosegue e il client vede esattamente cosa è andato a
            // buon fine e cosa no.
            log.error("BookingService - createBooking : Error creating booking", e);
            return new BookingInsertStatus(chrDTO.getConferenceHallId(), "Error while creating this booking. Try again later.");
        }
    }

    private BookingInsertStatus checkBookingRequest(UUID conferenceHallId, LocalDateTime start, LocalDateTime end) {
        return checkBookingRequest(conferenceHallId, start, end, null);
    }

    private BookingInsertStatus checkBookingRequest(UUID conferenceHallId, LocalDateTime start, LocalDateTime end, UUID excludeBookingId) {
        if (!conferenceHallService.conferenceHallAvailable(conferenceHallId)) {
            log.warn("BookingService - checkBookingRequest : Conference hall {} is not enabled", conferenceHallId);
            return new BookingInsertStatus(conferenceHallId, "Conference hall is not available");
        }
        if (bookingRepository.existsOverlapping(conferenceHallId, start, end, excludeBookingId)) {
            log.warn("BookingService - checkBookingRequest : Conference hall {} already booked between {} and {}", conferenceHallId, start, end);
            return new BookingInsertStatus(conferenceHallId, "Conference hall already booked for the selected time range");
        }
        return null;
    }

    public DetailBookingDTO getBookingById(UUID id) {
        log.info("BookingService - getBookingById : Fetching booking {}", id);
        return DetailBookingDTO.of(fetchBookingById(id));
    }

    public PagedResultDTO<BaseDetailBookingDTO> findAllBookings(BookingSearchRequest request) {
        log.info("BookingService - findAllBookings : Searching bookings, page {} size {}", request.getPage(), request.getSize());
        UUID userConstraint = resolveUserConstraint(request);
        PagedList<Booking> pagedList = bookingRepository.search(request, userConstraint);
        return PagedResultDTO.of(pagedList, BaseDetailBookingDTO::of);
    }

    public List<BaseDetailBookingDTO> findBookingsByUserId(UUID userId) {
        log.info("BookingService - findBookingsByUserId : Fetching bookings for user {}", userId);
        return bookingRepository.findByUserId(userId).stream()
                .map(BaseDetailBookingDTO::of)
                .toList();
    }

    public BookingInsertStatus updateBooking(UUID id, UpdateBookingDTO dto) {
        return updateBooking(fetchBookingById(id), dto);
    }

    public BookingInsertStatus updateBooking(UUID userId, UUID id, UpdateBookingDTO dto) {
        return updateBooking(fetchBookingOwnedBy(userId, id), dto);
    }

    private BookingInsertStatus updateBooking(Booking booking, UpdateBookingDTO dto) {
        log.info("BookingService - updateBooking : Rescheduling booking {}", booking.getId());
        BookingInsertStatus notModifiable = checkModifiable(booking, dto.getStartDateTime(), dto.getEndDateTime());
        if (notModifiable != null) {
            return notModifiable;
        }
        try (Transaction tx = database.beginTransaction()) {
            dto.toUpdate(booking);
            bookingRepository.update(booking, tx);
            tx.commit();
            log.info("BookingService - updateBooking : Updated booking {}", booking.getId());
            return null;
        } catch (Exception e) {
            if (ServiceException.isOverlapViolation(e)) {
                log.warn("BookingService - updateBooking : Overlap detected at database level for booking {}", booking.getId());
                return new BookingInsertStatus(booking.getConferenceHall().getId(), "Conference hall already booked for the selected time range");
            }
            log.error("BookingService - updateBooking : Error updating booking {}", booking.getId(), e);
            throw new ServiceException("Error while updating booking. Try again later.");
        }
    }

    public void deleteBooking(UUID id) {
        deleteBooking(fetchBookingById(id));
    }

    public void deleteBooking(UUID userId, UUID id) {
        deleteBooking(fetchBookingOwnedBy(userId, id));
    }

    private void deleteBooking(Booking booking) {
        log.info("BookingService - deleteBooking : Deleting booking {}", booking.getId());
        try (Transaction tx = database.beginTransaction()) {
            bookingRepository.delete(booking.getId(), tx);
            tx.commit();
        } catch (Exception e) {
            log.error("BookingService - deleteBooking : Error deleting booking {}", booking.getId(), e);
            throw new ServiceException("Error while deleting booking. Try again later.");
        }
    }

    private Booking fetchBookingById(UUID id) {
        Booking booking = bookingRepository.getBookingById(id);
        jwtInspector.checkAccessAllowed(booking.getUser().getId());
        return booking;
    }

    private Booking fetchBookingOwnedBy(UUID userId, UUID id) {
        Booking booking = bookingRepository.getBookingById(id);
        if (!booking.getUser().getId().equals(userId)) {
            log.error("BookingService - fetchBookingOwnedBy : Booking {} does not belong to user {}", id, userId);
            throw new ServiceException("Booking does not belong to the given user");
        }
        return booking;
    }

    private BookingInsertStatus checkModifiable(Booking booking, LocalDateTime newStart, LocalDateTime newEnd) {
        BookingInsertStatus badRequest = checkBookingRequest(booking.getConferenceHall().getId(), newStart, newEnd, booking.getId());
        if (badRequest != null) {
            return badRequest;
        }

        if (!jwtInspector.hasRole(Role.CUSTOMER)) {
            return null;
        }
        if (booking.isPaid()) {
            return new BookingInsertStatus(booking.getConferenceHall().getId(), "Booking already paid, cannot be modified");
        }
        if (!DateTimeUtils.isMoreThan24HoursAway(booking.getStartDateTime())) {
            return new BookingInsertStatus(booking.getConferenceHall().getId(), "Booking starts in less than 24 hours, cannot be modified");
        }
        return null;
    }

    private UUID resolveUserConstraint(BookingSearchRequest request) {
        if (jwtInspector.hasRole(Role.ADMIN) || jwtInspector.hasRole(Role.ORGANIZER)) {
            return request.getUserId();
        }
        return jwtInspector.getSubject();
    }

}
