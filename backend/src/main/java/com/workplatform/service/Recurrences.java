package com.workplatform.service;

import com.workplatform.entity.Enums.Recurrence;
import java.time.LocalDate;

public final class Recurrences {
    private Recurrences() {}

    public static LocalDate next(LocalDate from, Recurrence rule, int interval) {
        int n = Math.max(1, interval);
        return switch (rule) {
            case DAILY -> from.plusDays(n);
            case WEEKLY -> from.plusWeeks(n);
            case MONTHLY -> from.plusMonths(n);
            case NONE -> from;
        };
    }

    /** Next occurrence strictly after {@code from}, skipping any that already lie in the past relative to {@code today}. */
    public static LocalDate nextUpcoming(LocalDate from, Recurrence rule, int interval, LocalDate today) {
        LocalDate d = next(from, rule, interval);
        for (int guard = 0; d.isBefore(today) && guard < 3700; guard++) d = next(d, rule, interval);
        return d;
    }
}
