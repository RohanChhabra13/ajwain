package com.workplatform.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter @Entity @Table(name = "priorities")
public class Priority extends BaseEntity {
    @Column(nullable = false) private String name;
    /** Higher = more important. Used for sorting. */
    @Column(nullable = false) private int level;
    private String color;
    @ManyToOne(optional = false, fetch = FetchType.LAZY) private Workspace workspace;
}
