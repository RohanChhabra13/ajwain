package com.workplatform.service;

import com.workplatform.dto.Dtos.*;
import com.workplatform.entity.*;

final class Mapper {
    private Mapper() {}

    static UserSummary user(User u) { return u == null ? null : new UserSummary(u.getId(), u.getName(), u.getEmail()); }

    static ProjectResponse project(Project p) {
        return new ProjectResponse(p.getId(), p.getName(), p.getDescription(), p.getStatus(), p.getWorkspace().getId());
    }

    static CommentResponse comment(Comment c) { return new CommentResponse(c.getId(), c.getBody(), user(c.getAuthor()), c.getCreatedAt()); }

    static ActivityResponse activity(Activity a) {
        return new ActivityResponse(a.getId(), a.getType(), a.getMessage(), user(a.getActor()),
                a.getTask() == null ? null : a.getTask().getId(), a.getCreatedAt());
    }

    static StatusDto status(Status s) { return new StatusDto(s.getId(), s.getName(), s.getColor(), s.getPosition(), s.getCategory()); }
    static PriorityDto priority(Priority p) { return new PriorityDto(p.getId(), p.getName(), p.getLevel(), p.getColor()); }

    static CustomFieldDto field(CustomField f) {
        java.util.List<String> opts = f.getOptions() == null || f.getOptions().isBlank()
                ? java.util.List.of() : java.util.Arrays.asList(f.getOptions().split("\n"));
        return new CustomFieldDto(f.getId(), f.getName(), f.getType(), opts, f.getPosition());
    }

    static AttachmentResponse attachment(Attachment a) {
        return new AttachmentResponse(a.getId(), a.getFileName(), a.getContentType(), a.getSize(), user(a.getUploadedBy()), a.getCreatedAt());
    }

    static NotificationResponse notification(Notification n) {
        return new NotificationResponse(n.getId(), n.getType(), n.getMessage(), n.getTaskId(), n.getProjectId(), n.isRead(), n.getCreatedAt());
    }
}
