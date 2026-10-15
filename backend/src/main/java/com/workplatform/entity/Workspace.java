package com.workplatform.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter @Entity @Table(name = "workspaces")
public class Workspace extends BaseEntity {
    @Column(nullable = false) private String name;
    @ManyToOne(optional = false, fetch = FetchType.LAZY) private Organization organization;
}
