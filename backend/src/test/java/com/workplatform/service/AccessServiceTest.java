package com.workplatform.service;

import com.workplatform.entity.*;
import com.workplatform.entity.Enums.OrgRole;
import com.workplatform.exception.ApiException;
import com.workplatform.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import java.util.Optional;
import static com.workplatform.service.TestData.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AccessServiceTest {
    @Mock MembershipRepository memberships;
    @Mock WorkspaceMemberRepository workspaceMembers;
    @Mock WorkspaceRepository workspaces;
    @Mock ProjectRepository projects;
    @Mock TaskRepository tasks;
    @Mock EpicRepository epics;
    @InjectMocks AccessService access;

    Organization org = org(1);
    Workspace ws = workspace(10, org);
    User alice = user(1, "Alice");

    @BeforeEach void setUp() { when(workspaces.findById(10L)).thenReturn(Optional.of(ws)); }

    @Test void ownerCanAccessAnyWorkspaceInTheirOrganization() {
        when(memberships.findByUserIdAndOrganizationId(1L, 1L)).thenReturn(Optional.of(membership(alice, org, OrgRole.OWNER)));
        assertSame(ws, access.requireWorkspace(1L, 10L));
    }

    @Test void plainMemberNeedsToBeAddedToTheWorkspace() {
        when(memberships.findByUserIdAndOrganizationId(1L, 1L)).thenReturn(Optional.of(membership(alice, org, OrgRole.MEMBER)));
        when(workspaceMembers.existsByWorkspaceIdAndUserId(10L, 1L)).thenReturn(false);
        assertEquals(403, assertThrows(ApiException.class, () -> access.requireWorkspace(1L, 10L)).getStatus().value());

        when(workspaceMembers.existsByWorkspaceIdAndUserId(10L, 1L)).thenReturn(true);
        assertSame(ws, access.requireWorkspace(1L, 10L));
    }

    @Test void outsidersAreRejected() {
        when(memberships.findByUserIdAndOrganizationId(2L, 1L)).thenReturn(Optional.empty());
        assertThrows(ApiException.class, () -> access.requireWorkspace(2L, 10L));
        assertFalse(access.canAccessWorkspace(2L, ws));
    }

    @Test void managerChecksRejectPlainMembers() {
        when(memberships.findByUserIdAndOrganizationId(1L, 1L)).thenReturn(Optional.of(membership(alice, org, OrgRole.MEMBER)));
        assertThrows(ApiException.class, () -> access.requireWorkspaceManager(1L, 10L));
        when(memberships.findByUserIdAndOrganizationId(1L, 1L)).thenReturn(Optional.of(membership(alice, org, OrgRole.ADMIN)));
        assertSame(ws, access.requireWorkspaceManager(1L, 10L));
    }

    @Test void unknownWorkspaceIsNotFound() {
        when(workspaces.findById(99L)).thenReturn(Optional.empty());
        assertEquals(404, assertThrows(ApiException.class, () -> access.requireWorkspace(1L, 99L)).getStatus().value());
    }
}
