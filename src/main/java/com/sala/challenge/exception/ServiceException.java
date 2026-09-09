package com.sala.challenge.exception;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.postgresql.util.PSQLException;
import java.util.Objects;
import jakarta.persistence.PersistenceException;

@Slf4j
public class ServiceException extends RuntimeException {

    private static final String EXCLUSION_VIOLATION_SQLSTATE = "23P01";
    /**
     * Sotto inserimenti concorrenti reali, Postgres può risolvere il controllo
     * del vincolo EXCLUDE (GiST) con un deadlock invece che con una pulita
     * exclusion_violation: entrambe le transazioni finiscono per attendersi a
     * vicenda mentre verificano il vincolo sulla tupla dell'altra. È un
     * comportamento documentato di GiST sotto concorrenza, non un bug
     * applicativo — qui è trattato come lo stesso segnale "slot in conflitto",
     * senza introdurre un lock pessimistico (scelta di design confermata).
     */
    private static final String DEADLOCK_DETECTED_SQLSTATE = "40P01";

    public ServiceException() {
        super();
    }

    public ServiceException(String message) {
        super(message);
    }

    public ServiceException(Throwable cause) {
        super(cause);
    }

    public ServiceException(String message, Throwable cause) {
        super(message, cause);
    }

    public ServiceException(String message, Throwable cause,
                            boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public static ServiceException buildOrThrow(Throwable e, String message) {
        if (e instanceof ServiceException)
            return new ServiceException(ExceptionUtils.getRootCause(e).getMessage());
        return new ServiceException(message);
    }

    public static ServiceException fromPersistenceException(PersistenceException ex) {
        Throwable cause = ex.getCause();
        if (cause instanceof PSQLException pex)
            log.error("Database error: {{}}", Objects.requireNonNull(pex.getServerErrorMessage()).getMessage());
        return new ServiceException("Database operation failed");
    }

    public static boolean isOverlapViolation(Throwable ex) {
        Throwable cause = ExceptionUtils.getRootCause(ex);
        if (!(cause instanceof PSQLException pex)) {
            return false;
        }
        String sqlState = pex.getSQLState();
        return EXCLUSION_VIOLATION_SQLSTATE.equals(sqlState) || DEADLOCK_DETECTED_SQLSTATE.equals(sqlState);
    }
}
