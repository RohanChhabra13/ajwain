package com.workplatform.service;

import com.workplatform.dto.Dtos.*;
import com.workplatform.entity.*;
import com.workplatform.entity.Enums.*;
import com.workplatform.exception.ApiException;
import com.workplatform.repository.TaskRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import static com.workplatform.service.TestData.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ReportServiceTest {
    @Mock AccessService access;
    @Mock TaskRepository tasks;
    @InjectMocks ReportService service;

    @Test void summarisesCompletedOverdueAndCycleTime() {
        Workspace ws = workspace(10, org(1));
        Project p = project(100, ws);
        Status todo = status(1, ws, StatusCategory.TODO, "To do");
        Status done = status(2, ws, StatusCategory.DONE, "Done");
        Priority high = priority(3, ws, 3, "High");
        User alice = user(1, "Alice");
        Instant now = Instant.now();

        Task finished = task(1, p, done, high, alice);
        finished.setAssignee(alice);
        finished.setCreatedAt(now.minus(4, ChronoUnit.DAYS));
        finished.setCompletedAt(now);
        Task late = task(2, p, todo, high, alice);
        late.setAssignee(alice);
        late.setCreatedAt(now.minus(2, ChronoUnit.DAYS));
        late.setDueDate(LocalDate.now().minusDays(1));
        when(tasks.findByProjectWorkspaceId(10L)).thenReturn(List.of(finished, late));

        LocalDate to = LocalDate.now(ZoneOffset.UTC).plusDays(1);
        ReportResponse r = service.report(1L, 10L, to.minusDays(10), to);

        assertEquals(2, r.created());
        assertEquals(1, r.completed());
        assertEquals(1, r.openOverdue());
        assertEquals(4.0, r.avgCycleDays(), 0.1);
        assertEquals(11, r.daily().size());
        assertEquals(1, r.members().size());
        assertEquals(1, r.members().get(0).completed());
        assertEquals(1, r.members().get(0).overdue());
        assertEquals(2, r.projects().get(0).total());
        assertEquals(1, r.projects().get(0).done());
        assertEquals(1, r.openByPriority().get(0).count());
    }

    @Test void rejectsInvertedOrHugeRanges() {
        LocalDate d = LocalDate.of(2026, 1, 10);
        assertThrows(ApiException.class, () -> service.report(1L, 10L, d, d.minusDays(1)));
        assertThrows(ApiException.class, () -> service.report(1L, 10L, d.minusYears(3), d));
    }
}
