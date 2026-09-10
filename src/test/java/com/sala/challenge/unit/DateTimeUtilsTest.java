package com.sala.challenge.unit;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import com.sala.challenge.util.DateTimeUtils;

class DateTimeUtilsTest {

    @Test
    void minutesBetween_returnsTotalMinutes() {
        LocalDateTime start = LocalDateTime.of(2026, 1, 1, 9, 0);
        LocalDateTime end = LocalDateTime.of(2026, 1, 1, 10, 45);

        long minutes = DateTimeUtils.minutesBetween(start, end);

        assertThat(minutes).isEqualTo(105L);
    }

    @Test
    void isMoreThan24HoursAway_startMoreThan24HoursInFuture_returnsTrue() {
        LocalDateTime startDateTime = LocalDateTime.now().plusHours(25);

        boolean result = DateTimeUtils.isMoreThan24HoursAway(startDateTime);

        assertThat(result).isTrue();
    }

    @Test
    void isMoreThan24HoursAway_startLessThan24HoursInFuture_returnsFalse() {
        LocalDateTime startDateTime = LocalDateTime.now().plusHours(1);

        boolean result = DateTimeUtils.isMoreThan24HoursAway(startDateTime);

        assertThat(result).isFalse();
    }

    @Test
    void isMoreThan24HoursAway_startInThePast_returnsFalse() {
        LocalDateTime startDateTime = LocalDateTime.now().minusHours(1);

        boolean result = DateTimeUtils.isMoreThan24HoursAway(startDateTime);

        assertThat(result).isFalse();
    }
}
