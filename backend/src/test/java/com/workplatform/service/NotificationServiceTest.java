package com.workplatform.service;

import com.workplatform.entity.*;
import com.workplatform.exception.ApiException;
import com.workplatform.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import java.util.Optional;
import static com.workplatform.service.TestData.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class NotificationServiceTest {
    @Mock NotificationRepository notifications;
    @Mock UserRepository users;
    @InjectMocks NotificationService service;

    private Task task() {
        Workspace ws = workspace(10, org(1));
        return task(5, project(100, ws), status(1, ws, Enums.StatusCategory.TODO, "To do"), priority(1, ws, 1, "Low"), user(1, "Alice"));
    }

    @Test void neverNotifiesTheActorOrNobody() {
        service.notify(1L, 1L, "X", "msg", null);
        service.notify(null, 1L, "X", "msg", null);
        verify(notifications, never()).save(any());
    }

    @Test void savesNotificationLinkedToTheTask() {
        User bob = user(2, "Bob");
        when(users.getReferenceById(2L)).thenReturn(bob);
        service.notify(2L, 1L, "TASK_ASSIGNED", "You were assigned X", task());
        ArgumentCaptor<Notification> c = ArgumentCaptor.forClass(Notification.class);
        verify(notifications).save(c.capture());
        assertSame(bob, c.getValue().getUser());
        assertEquals(5L, c.getValue().getTaskId());
        assertEquals(100L, c.getValue().getProjectId());
        assertFalse(c.getValue().isRead());
        assertNull(c.getValue().getDedupeKey());
    }

    @Test void notifyOnceIsIdempotentPerKey() {
        when(users.getReferenceById(2L)).thenReturn(user(2, "Bob"));
        when(notifications.existsByDedupeKey("K")).thenReturn(false, true);
        assertTrue(service.notifyOnce(2L, "DUE_TODAY", "m", task(), "K"));
        assertFalse(service.notifyOnce(2L, "DUE_TODAY", "m", task(), "K"));
        verify(notifications, times(1)).save(any(Notification.class));
    }

    @Test void onlyTheOwnerCanMarkANotificationRead() {
        Notification n = new Notification();
        n.setUser(user(2, "Bob"));
        when(notifications.findById(9L)).thenReturn(Optional.of(n));
        assertThrows(ApiException.class, () -> service.markRead(3L, 9L));
        assertFalse(n.isRead());
        service.markRead(2L, 9L);
        assertTrue(n.isRead());
    }
}
