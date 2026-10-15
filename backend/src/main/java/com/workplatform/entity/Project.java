package com.workplatform.entity;

import com.workplatform.entity.Enums.ProjectStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter @Entity @Table(name = "projects")
public class Project extends BaseEntity {
    @Column(nullable = false) private String name;
    @Column(length = 4000) private String description;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private ProjectStatus status = ProjectStatus.ACTIVE;
    @ManyToOne(optional = false, fetch = FetchType.LAZY) private Workspace workspace;
}
