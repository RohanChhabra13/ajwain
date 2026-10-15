package com.workplatform.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter @Entity @Table(name = "comments")
public class Comment extends BaseEntity {
    @Column(nullable = false, length = 4000) private String body;
    @ManyToOne(optional = false, fetch = FetchType.LAZY) private Task task;
    @ManyToOne(optional = false, fetch = FetchType.LAZY) private User author;
}
