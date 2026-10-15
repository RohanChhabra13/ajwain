package com.workplatform.entity;

import com.workplatform.entity.Enums.OrgRole;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/** A user's membership (and role) in an organization. */
@Getter @Setter @Entity
@Table(name = "memberships", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "organization_id"}))
public class Membership extends BaseEntity {
    @ManyToOne(optional = false, fetch = FetchType.LAZY) private User user;
    @ManyToOne(optional = false, fetch = FetchType.LAZY) private Organization organization;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private OrgRole role = OrgRole.MEMBER;
}
