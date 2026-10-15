package com.workplatform.entity;

import com.workplatform.entity.Enums.EpicStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;

@Getter @Setter @Entity @Table(name = "epics")
public class Epic extends BaseEntity {
    @Column(nullable = false) private String name;
    @Column(length = 4000) private String description;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private EpicStatus status = EpicStatus.OPEN;
    private LocalDate dueDate;
    @ManyToOne(optional = false, fetch = FetchType.LAZY) private Project project;
}
