package com.sala.challenge.unit;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import com.sala.challenge.util.PricingUtils;

class PricingUtilsTest {

    @Test
    void computeTotalCost_exactHourMinutes_returnsPriceTimesHours() {
        BigDecimal pricePerHour = BigDecimal.valueOf(10);

        BigDecimal totalCost = PricingUtils.computeTotalCost(pricePerHour, 60L);

        assertThat(totalCost).isEqualByComparingTo("10.00");
    }

    @Test
    void computeTotalCost_ninetyMinutes_returnsOneAndHalfHoursCost() {
        BigDecimal pricePerHour = BigDecimal.valueOf(10);

        BigDecimal totalCost = PricingUtils.computeTotalCost(pricePerHour, 90L);

        assertThat(totalCost).isEqualByComparingTo("15.00");
    }

    @Test
    void computeTotalCost_minutesNotEvenlyDivisibleByHour_roundsHalfUpToTwoDecimals() {
        BigDecimal pricePerHour = BigDecimal.valueOf(10);

        BigDecimal totalCost = PricingUtils.computeTotalCost(pricePerHour, 100L);

        assertThat(totalCost).isEqualByComparingTo("16.70");
    }

    @Test
    void computeTotalCost_zeroMinutes_returnsZero() {
        BigDecimal pricePerHour = BigDecimal.valueOf(10);

        BigDecimal totalCost = PricingUtils.computeTotalCost(pricePerHour, 0L);

        assertThat(totalCost).isEqualByComparingTo("0.00");
    }

    @Test
    void computeTotalCost_fromStartAndEndDateTime_delegatesToMinutesBetweenOverload() {
        BigDecimal pricePerHour = BigDecimal.valueOf(10);
        LocalDateTime start = LocalDateTime.of(2026, 1, 1, 9, 0);
        LocalDateTime end = LocalDateTime.of(2026, 1, 1, 10, 30);

        BigDecimal totalCost = PricingUtils.computeTotalCost(pricePerHour, start, end);

        assertThat(totalCost).isEqualByComparingTo("15.00");
    }
}
