package com.workplatform.service;

import com.workplatform.entity.Enums.*;
import com.workplatform.entity.Task;
import com.workplatform.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;

/** Daily in-app reminders for tasks that are due today or overdue. Safe to run repeatedly (idempotent per task/day). */
@Component
@RequiredArgsConstructor
public class DueReminderJob {
    private final TaskRepository tasks;
    private final NotificationService notifications;

    @Scheduled(cron = "${app.reminders.cron:0 0 7 * * *}")
    @Transactional
    public void run() { remind(LocalDate.now()); }

    /** @return number of notifications created */
    public int remind(LocalDate today) {
        int created = 0;
        for (Task t : tasks.findDueForReminders(today, StatusCategory.DONE, ProjectStatus.ACTIVE)) {
            Long assignee = t.getAssignee().getId();
            boolean dueToday = t.getDueDate().isEqual(today);
            boolean ok = dueToday
                    ? notifications.notifyOnce(assignee, "DUE_TODAY", "\"" + t.getTitle() + "\" is due today", t, "DUE_TODAY:" + t.getId() + ":" + today)
                    : notifications.notifyOnce(assignee, "OVERDUE", "\"" + t.getTitle() + "\" is overdue (was due " + t.getDueDate() + ")", t, "OVERDUE:" + t.getId());
            if (ok) created++;
        }
        return created;
    }
}
