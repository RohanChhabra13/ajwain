package com.workplatform.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/** Grants an organization MEMBER access to a specific workspace (owners/admins see every workspace). */
@Getter @Setter @Entity
@Table(name = "workspace_members", uniqueConstraints = @UniqueConstraint(columnNames = {"workspace_id", "user_id"}))
public class WorkspaceMember extends BaseEntity {
    @ManyToOne(optional = false, fetch = FetchType.LAZY) private Workspace workspace;
    @ManyToOne(optional = false, fetch = FetchType.LAZY) private User user;
}
