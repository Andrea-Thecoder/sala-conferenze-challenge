package com.naxos.challenge.util;

import java.time.Duration;
import java.time.LocalDateTime;

public class DateTimeUtils {

    private static final long MINUTES_IN_A_DAY = 24 * 60;

    public static long hoursBetween(LocalDateTime start, LocalDateTime end) {
        return Duration.between(start, end).toHours();
    }

    public static int minutesPartBetween(LocalDateTime start, LocalDateTime end) {
        return Duration.between(start, end).toMinutesPart();
    }

    public static long minutesBetween(LocalDateTime start, LocalDateTime end) {
        return Duration.between(start, end).toMinutes();
    }

    public static boolean isMoreThan24HoursAway(LocalDateTime startDateTime) {
        return DateTimeUtils.minutesBetween(LocalDateTime.now(), startDateTime) > MINUTES_IN_A_DAY;
    }
}
