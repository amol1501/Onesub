package com.onesub.util;

import com.onesub.enums.BillingCycle;

import java.time.LocalDate;
import java.time.YearMonth;

public final class BillingDateCalculator {
    private BillingDateCalculator() {
    }

    public static LocalDate calculateInitialRenewal(LocalDate startDate, BillingCycle cycle) {
        if (startDate == null) {
            throw new IllegalArgumentException("Start date is required");
        }
        return addCycle(startDate, cycle);
    }

    public static LocalDate calculateNext(LocalDate currentRenewal, BillingCycle cycle) {
        return addCycle(currentRenewal, cycle);
    }

    public static LocalDate addCycle(LocalDate date, BillingCycle cycle) {
        int originalDay = date.getDayOfMonth();
        LocalDate result;
        switch (cycle) {
            case WEEKLY -> result = date.plusWeeks(1);
            case MONTHLY -> result = addMonthsClamped(date, 1);
            case QUARTERLY -> result = addMonthsClamped(date, 3);
            case YEARLY -> result = date.plusYears(1);
            default -> throw new IllegalArgumentException("Unsupported billing cycle");
        }

        if (cycle == BillingCycle.YEARLY && date.isLeapYear()
                && date.getMonthValue() == 2 && originalDay == 29) {
            int maxDay = YearMonth.of(result.getYear(), result.getMonthValue()).lengthOfMonth();
            result = result.withDayOfMonth(Math.min(originalDay, maxDay));
        }
        return result;
    }

    private static LocalDate addMonthsClamped(LocalDate date, int months) {
        LocalDate base = date.plusMonths(months);
        int maxDay = YearMonth.from(base).lengthOfMonth();
        return base.withDayOfMonth(Math.min(date.getDayOfMonth(), maxDay));
    }
}
