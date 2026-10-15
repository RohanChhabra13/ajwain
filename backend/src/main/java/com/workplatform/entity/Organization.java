package com.workplatform.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter @Entity @Table(name = "organizations")
public class Organization extends BaseEntity {
    @Column(nullable = false) private String name;
}
