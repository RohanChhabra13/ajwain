package com.workplatform.service;

import com.workplatform.entity.*;
import com.workplatform.entity.Enums.*;
import com.workplatform.repository.TaskRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import java.time.LocalDate;
import java.util.List;
import static com.workplatform.service.TestData.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DueReminderJobTest {
    @Mock TaskRepository tasks;
    @Mock NotificationService notifications;
    @InjectMocks DueReminderJob job;

    @Test void remindsAboutTasksDueTodayAndOverdueWithStableKeys() {
        LocalDate today = LocalDate.of(2026, 5, 20);
        Workspace ws = workspace(10, org(1));
        Project p = project(100, ws);
        Status todo = status(1, ws, StatusCategory.TODO, "To do");
        Priority prio = priority(1, ws, 1, "Low");
        User bob = user(2, "Bob");

        Task dueToday = task(10, p, todo, prio, bob); dueToday.setAssignee(bob); dueToday.setDueDate(today);
        Task overdue = task(11, p, todo, prio, bob); overdue.setAssignee(bob); overdue.setDueDate(today.minusDays(3));
        when(tasks.findDueForReminders(today, StatusCategory.DONE, ProjectStatus.ACTIVE)).thenReturn(List.of(dueToday, overdue));
        when(notifications.notifyOnce(any(), anyString(), anyString(), any(), anyString())).thenReturn(true);

        assertEquals(2, job.remind(today));

        ArgumentCaptor<String> keys = ArgumentCaptor.forClass(String.class);
        verify(notifications, times(2)).notifyOnce(eq(2L), anyString(), anyString(), any(Task.class), keys.capture());
        assertTrue(keys.getAllValues().contains("DUE_TODAY:10:2026-05-20"));
        assertTrue(keys.getAllValues().contains("OVERDUE:11"));
    }

    @Test void countsOnlyNotificationsThatWereActuallyCreated() {
        LocalDate today = LocalDate.of(2026, 5, 20);
        Workspace ws = workspace(10, org(1));
        User bob = user(2, "Bob");
        Task t = task(10, project(100, ws), status(1, ws, StatusCategory.TODO, "To do"), priority(1, ws, 1, "Low"), bob);
        t.setAssignee(bob); t.setDueDate(today);
        when(tasks.findDueForReminders(today, StatusCategory.DONE, ProjectStatus.ACTIVE)).thenReturn(List.of(t));
        when(notifications.notifyOnce(any(), anyString(), anyString(), any(), anyString())).thenReturn(false);
        assertEquals(0, job.remind(today));
    }
}
