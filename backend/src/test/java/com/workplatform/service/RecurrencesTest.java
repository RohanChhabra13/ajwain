package com.workplatform.service;

import com.workplatform.entity.Enums.Recurrence;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.assertEquals;

class RecurrencesTest {
    @Test void dailyWeeklyMonthly() {
        LocalDate d = LocalDate.of(2026, 1, 31);
        assertEquals(LocalDate.of(2026, 2, 1), Recurrences.next(d, Recurrence.DAILY, 1));
        assertEquals(LocalDate.of(2026, 2, 14), Recurrences.next(d, Recurrence.WEEKLY, 2));
        assertEquals(LocalDate.of(2026, 2, 28), Recurrences.next(d, Recurrence.MONTHLY, 1));
    }

    @Test void intervalBelowOneIsTreatedAsOne() {
        assertEquals(LocalDate.of(2026, 3, 2), Recurrences.next(LocalDate.of(2026, 3, 1), Recurrence.DAILY, 0));
    }

    @Test void nextUpcomingSkipsOccurrencesInThePast() {
        LocalDate today = LocalDate.of(2026, 6, 10);
        LocalDate lastDue = LocalDate.of(2026, 6, 1);
        assertEquals(LocalDate.of(2026, 6, 10), Recurrences.nextUpcoming(lastDue, Recurrence.DAILY, 1, today));
        assertEquals(LocalDate.of(2026, 6, 15), Recurrences.nextUpcoming(lastDue, Recurrence.WEEKLY, 1, today));
    }
}
