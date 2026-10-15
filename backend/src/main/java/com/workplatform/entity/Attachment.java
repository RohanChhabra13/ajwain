package com.workplatform.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter @Entity @Table(name = "attachments")
public class Attachment extends BaseEntity {
    @Column(nullable = false) private String fileName;
    private String contentType;
    private long size;
    /** Opaque name of the file on disk (never derived from user input). */
    @Column(nullable = false) private String storageKey;
    @ManyToOne(optional = false, fetch = FetchType.LAZY) private Task task;
    @ManyToOne(optional = false, fetch = FetchType.LAZY) private User uploadedBy;
}
