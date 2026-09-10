package com.sala.challenge.unit;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.postgresql.util.PSQLException;
import org.postgresql.util.PSQLState;

import com.sala.challenge.exception.ServiceException;

class ServiceExceptionTest {

    @Test
    void isOverlapViolation_causeIsExclusionViolation_returnsTrue() {
        PSQLException exclusionViolation = new PSQLException("conflicting booking range", PSQLState.EXCLUSION_VIOLATION);

        boolean result = ServiceException.isOverlapViolation(exclusionViolation);

        assertThat(result).isTrue();
    }

    @Test
    void isOverlapViolation_causeIsDeadlockDetected_returnsTrue() {
        PSQLException deadlock = new PSQLException("deadlock detected", PSQLState.DEADLOCK_DETECTED);

        boolean result = ServiceException.isOverlapViolation(deadlock);

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
