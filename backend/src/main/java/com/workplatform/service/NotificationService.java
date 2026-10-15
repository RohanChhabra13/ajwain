package com.workplatform.service;

import com.workplatform.dto.Dtos.NotificationResponse;
import com.workplatform.entity.*;
import com.workplatform.exception.ApiException;
import com.workplatform.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class NotificationService {
    private final NotificationRepository notifications;
    private final UserRepository users;

    /** Notify a user about something someone else did. Never notifies the actor about their own action. */
    public void notify(Long recipientId, Long actorId, String type, String message, Task task) {
        if (recipientId == null || recipientId.equals(actorId)) return;
        save(recipientId, type, message, task, null);
    }

    /** Idempotent notification used by scheduled reminders. Returns true if a new one was created. */
    public boolean notifyOnce(Long recipientId, String type, String message, Task task, String dedupeKey) {
        if (recipientId == null || notifications.existsByDedupeKey(dedupeKey)) return false;
        save(recipientId, type, message, task, dedupeKey);
        return true;
    }

    private void save(Long recipientId, String type, String message, Task task, String dedupeKey) {
        Notification n = new Notification();
        n.setUser(users.getReferenceById(recipientId));
        n.setType(type);
        n.setMessage(message);
        if (task != null) { n.setTaskId(task.getId()); n.setProjectId(task.getProject().getId()); }
        n.setDedupeKey(dedupeKey);
        notifications.save(n);
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> list(Long userId) {
        return notifications.findTop50ByUserIdOrderByCreatedAtDesc(userId).stream().map(Mapper::notification).toList();
    }

    @Transactional(readOnly = true)
    public long unreadCount(Long userId) { return notifications.countByUserIdAndReadFalse(userId); }

    public void markRead(Long userId, Long id) {
        Notification n = notifications.findById(id).orElseThrow(() -> ApiException.notFound("Notification"));
        if (!n.getUser().getId().equals(userId)) throw ApiException.forbidden();
        n.setRead(true);
    }

    public void markAllRead(Long userId) { notifications.markAllRead(userId); }
}
