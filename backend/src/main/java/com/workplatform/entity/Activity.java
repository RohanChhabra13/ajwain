package com.workplatform.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter @Entity @Table(name = "activities")
public class Activity extends BaseEntity {
    @Column(nullable = false) private String type;
    @Column(nullable = false, length = 1000) private String message;
    @ManyToOne(fetch = FetchType.LAZY) private Task task;
    @ManyToOne(optional = false, fetch = FetchType.LAZY) private Project project;
    @ManyToOne(optional = false, fetch = FetchType.LAZY) private User actor;
}
