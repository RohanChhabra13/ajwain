package com.workplatform.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter @Entity
@Table(name = "tags", uniqueConstraints = @UniqueConstraint(columnNames = {"workspace_id", "name"}))
public class Tag extends BaseEntity {
    @Column(nullable = false) private String name;
    @ManyToOne(optional = false, fetch = FetchType.LAZY) private Workspace workspace;
}
