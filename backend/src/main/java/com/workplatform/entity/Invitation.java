package com.workplatform.entity;

import com.workplatform.entity.Enums.InviteStatus;
import com.workplatform.entity.Enums.OrgRole;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;

@Getter @Setter @Entity @Table(name = "invitations")
public class Invitation extends BaseEntity {
    @Column(nullable = false) private String email;
    @Column(nullable = false, unique = true) private String token;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private OrgRole role = OrgRole.MEMBER;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private InviteStatus status = InviteStatus.PENDING;
    @Column(nullable = false) private Instant expiresAt;
    @ManyToOne(optional = false, fetch = FetchType.LAZY) private Organization organization;
    @ManyToOne(optional = false, fetch = FetchType.LAZY) private Workspace workspace;
    @ManyToOne(optional = false, fetch = FetchType.LAZY) private User invitedBy;
}
