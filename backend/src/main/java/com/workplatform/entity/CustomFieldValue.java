package com.workplatform.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter @Entity
@Table(name = "custom_field_values", uniqueConstraints = @UniqueConstraint(columnNames = {"task_id", "field_id"}))
public class CustomFieldValue extends BaseEntity {
    @ManyToOne(optional = false, fetch = FetchType.LAZY) private Task task;
    @ManyToOne(optional = false, fetch = FetchType.LAZY) private CustomField field;
    @Column(name = "field_value", nullable = false, length = 2000) private String value;
}
