package com.workplatform.entity;

import com.workplatform.entity.Enums.FieldType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter @Entity @Table(name = "custom_fields")
public class CustomField extends BaseEntity {
    @Column(nullable = false) private String name;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private FieldType type = FieldType.TEXT;
    /** SELECT options, newline-separated. */
    @Column(length = 2000) private String options;
    @Column(nullable = false) private int position;
    @ManyToOne(optional = false, fetch = FetchType.LAZY) private Workspace workspace;
}
