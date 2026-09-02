package com.naxos.challenge.exception;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.postgresql.util.PSQLException;
import java.util.Objects;
import jakarta.persistence.PersistenceException;

@Slf4j
public class ServiceException extends RuntimeException {

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
}
