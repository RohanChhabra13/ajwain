package com.workplatform.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter @Entity
@Table(name = "notifications", indexes = @Index(columnList = "user_id, is_read"))
public class Notification extends BaseEntity {
    @ManyToOne(optional = false, fetch = FetchType.LAZY) private User user;
    @Column(nullable = false) private String type;
    @Column(nullable = false, length = 1000) private String message;
    private Long taskId;
    private Long projectId;
    @Column(name = "is_read", nullable = false) private boolean read = false;
    /** When set, only one notification with this key is ever created (used by reminders). */
    @Column(unique = true) private String dedupeKey;
}
