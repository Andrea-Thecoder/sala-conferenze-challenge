package com.sala.challenge.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.postgresql.util.PSQLException;
import org.postgresql.util.PSQLState;
import org.postgresql.util.ServerErrorMessage;

import com.sala.challenge.exception.ServiceException;

import jakarta.persistence.PersistenceException;

class ServiceExceptionTest {

    @Test
    void buildOrThrow_causeIsServiceExceptionWithoutFurtherCause_returnsServiceExceptionWithSameMessage() {
        ServiceException cause = new ServiceException("boom");

        ServiceException result = ServiceException.buildOrThrow(cause, "fallback message");

        assertThat(result.getMessage()).isEqualTo("boom");
    }

    @Test
    void buildOrThrow_causeIsServiceExceptionWrappingDeeperCause_returnsServiceExceptionWithRootCauseMessage() {
        ServiceException cause = new ServiceException("wrapper", new RuntimeException("root cause detail"));

        ServiceException result = ServiceException.buildOrThrow(cause, "fallback message");

        assertThat(result.getMessage()).isEqualTo("root cause detail");
    }

    @Test
    void buildOrThrow_causeIsNotServiceException_returnsServiceExceptionWithProvidedMessage() {
        RuntimeException cause = new RuntimeException("internal detail that should not leak");

        ServiceException result = ServiceException.buildOrThrow(cause, "fallback message");

        assertThat(result.getMessage()).isEqualTo("fallback message");
    }

    @Test
    void fromPersistenceException_causeIsNotPSQLException_returnsGenericDatabaseErrorMessage() {
        PersistenceException persistenceException = new PersistenceException(new RuntimeException("driver detail"));

        ServiceException result = ServiceException.fromPersistenceException(persistenceException);

        assertThat(result.getMessage()).isEqualTo("Database operation failed");
    }

    @Test
    void fromPersistenceException_causeIsPSQLException_returnsGenericDatabaseErrorMessage() {
        ServerErrorMessage serverErrorMessage = mock(ServerErrorMessage.class);
        when(serverErrorMessage.getMessage()).thenReturn("duplicate key value violates unique constraint");
        PSQLException psqlException = mock(PSQLException.class);
        when(psqlException.getServerErrorMessage()).thenReturn(serverErrorMessage);
        PersistenceException persistenceException = new PersistenceException(psqlException);

        ServiceException result = ServiceException.fromPersistenceException(persistenceException);

        assertThat(result.getMessage()).isEqualTo("Database operation failed");
    }

    @Test
    void isOverlapViolation_causeIsExclusionViolation_returnsTrue() {
        PSQLException exclusionViolation = new PSQLException("conflicting booking range", PSQLState.EXCLUSION_VIOLATION);

        boolean result = ServiceException.isOverlapViolation(exclusionViolation);

        assertThat(result).isTrue();
    }

    @Test
    void isOverlapViolation_causeIsDifferentSqlState_returnsFalse() {
        PSQLException uniqueViolation = new PSQLException("duplicate key", PSQLState.UNIQUE_VIOLATION);

        boolean result = ServiceException.isOverlapViolation(uniqueViolation);

        assertThat(result).isFalse();
    }

    @Test
    void isOverlapViolation_causeIsNotPSQLException_returnsFalse() {
        RuntimeException unrelatedException = new RuntimeException("not a database error");

        boolean result = ServiceException.isOverlapViolation(unrelatedException);

        assertThat(result).isFalse();
    }

    @Test
    void isOverlapViolation_exclusionViolationWrappedInOuterException_returnsTrue() {
        PSQLException exclusionViolation = new PSQLException("conflicting booking range", PSQLState.EXCLUSION_VIOLATION);
        RuntimeException wrapper = new RuntimeException("wrapped", exclusionViolation);

        boolean result = ServiceException.isOverlapViolation(wrapper);

        assertThat(result).isTrue();
    }
}
