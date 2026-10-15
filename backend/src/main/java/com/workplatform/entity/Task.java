package com.workplatform.entity;

import com.workplatform.entity.Enums.Recurrence;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Getter @Setter @Entity @Table(name = "tasks")
public class Task extends BaseEntity {
    @Column(nullable = false) private String title;
    @Column(length = 8000) private String description;
    @ManyToOne(optional = false, fetch = FetchType.LAZY) private Status status;
    @ManyToOne(optional = false, fetch = FetchType.LAZY) private Priority priority;
    private LocalDate dueDate;
    /** The day the assignee plans to work on it ("My Day"). */
    private LocalDate plannedDate;
    private Instant completedAt;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private Recurrence recurrence = Recurrence.NONE;
    @Column(nullable = false) private int recurrenceInterval = 1;
    /** True once the next occurrence has been generated, so re-opening/closing doesn't duplicate it. */
    @Column(nullable = false) private boolean recurrenceSpawned = false;
    @ManyToOne(optional = false, fetch = FetchType.LAZY) private Project project;
    @ManyToOne(fetch = FetchType.LAZY) private Epic epic;
    /** Non-null => this is a subtask. Only one level of nesting is allowed. */
    @ManyToOne(fetch = FetchType.LAZY) private Task parent;
    @ManyToOne(fetch = FetchType.LAZY) private User assignee;
    @ManyToOne(optional = false, fetch = FetchType.LAZY) private User createdBy;
    @ManyToMany
    @JoinTable(name = "task_tags", joinColumns = @JoinColumn(name = "task_id"), inverseJoinColumns = @JoinColumn(name = "tag_id"))
    private Set<Tag> tags = new HashSet<>();
}
