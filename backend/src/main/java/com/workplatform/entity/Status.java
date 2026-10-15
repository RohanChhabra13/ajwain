package com.workplatform.entity;

import com.workplatform.entity.Enums.StatusCategory;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/** A workflow status (column) configured per workspace. */
@Getter @Setter @Entity @Table(name = "task_statuses")
public class Status extends BaseEntity {
    @Column(nullable = false) private String name;
    private String color;
    @Column(nullable = false) private int position;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private StatusCategory category = StatusCategory.TODO;
    @ManyToOne(optional = false, fetch = FetchType.LAZY) private Workspace workspace;
}
