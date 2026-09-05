package com.naxos.challenge.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

public class PricingUtils {

    private static final short SCALE = 2;
    private static final short DIVIDER = 60;

    public static BigDecimal computeTotalCost(BigDecimal pricePerHour, LocalDateTime start, LocalDateTime end){
        return computeTotalCost(pricePerHour,DateTimeUtils.minutesBetween(start, end));
    }

    public static BigDecimal computeTotalCost(BigDecimal pricePerHour, long minutes) {
        BigDecimal hours = BigDecimal.valueOf(minutes).divide(BigDecimal.valueOf(DIVIDER), SCALE, RoundingMode.HALF_UP);
        return pricePerHour.multiply(hours);
    }
}
