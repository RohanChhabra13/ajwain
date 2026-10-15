package com.workplatform.service;

import com.workplatform.dto.Dtos.*;
import com.workplatform.entity.*;
import com.workplatform.entity.Enums.*;
import com.workplatform.exception.ApiException;
import com.workplatform.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import static com.workplatform.service.TestData.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class InvitationServiceTest {
    @Mock AccessService access;
    @Mock UserRepository users;
    @Mock MembershipRepository memberships;
    @Mock WorkspaceMemberRepository workspaceMembers;
    @Mock InvitationRepository invitations;
    @Mock NotificationService notifications;
    @InjectMocks InvitationService service;

    Organization org = org(1);
    Workspace ws = workspace(10, org);
    User owner = user(1, "Owner");

    @BeforeEach void setUp() {
        when(access.requireWorkspaceManager(1L, 10L)).thenReturn(ws);
        when(access.requireOrgMember(1L, 1L)).thenReturn(membership(owner, org, OrgRole.OWNER));
        when(users.getReferenceById(1L)).thenReturn(owner);
        when(invitations.save(any(Invitation.class))).thenAnswer(i -> i.getArgument(0));
        when(memberships.save(any(Membership.class))).thenAnswer(i -> i.getArgument(0));
    }

    @Test void unknownEmailGetsAnInvitationLink() {
        when(users.findByEmailIgnoreCase("new@example.com")).thenReturn(Optional.empty());
        when(invitations.findFirstByWorkspaceIdAndEmailIgnoreCaseAndStatus(10L, "new@example.com", InviteStatus.PENDING)).thenReturn(Optional.empty());

        InviteResult res = service.invite(1L, 10L, new InviteRequest("New@Example.com", null));

        assertTrue(res.invited());
        assertNull(res.member());
        assertEquals("new@example.com", res.invitation().email());
        assertEquals(OrgRole.MEMBER, res.invitation().role());
        assertNotNull(res.invitation().token());
        assertTrue(res.invitation().token().length() >= 20);
        assertTrue(res.invitation().expiresAt().isAfter(Instant.now().plus(10, ChronoUnit.DAYS)));
    }

    @Test void existingUserIsAddedDirectlyToOrganizationAndWorkspace() {
        User bob = user(2, "Bob");
        when(users.findByEmailIgnoreCase("bob@example.com")).thenReturn(Optional.of(bob));
        when(memberships.findByUserIdAndOrganizationId(2L, 1L)).thenReturn(Optional.empty());
        when(workspaceMembers.existsByWorkspaceIdAndUserId(10L, 2L)).thenReturn(false);

        InviteResult res = service.invite(1L, 10L, new InviteRequest("bob@example.com", OrgRole.MEMBER));

        assertFalse(res.invited());
        assertEquals("Bob", res.member().name());
        ArgumentCaptor<WorkspaceMember> wm = ArgumentCaptor.forClass(WorkspaceMember.class);
        verify(workspaceMembers).save(wm.capture());
        assertSame(bob, wm.getValue().getUser());
        verify(notifications).notify(eq(2L), eq(1L), eq("ADDED_TO_WORKSPACE"), anyString(), isNull());
        verify(invitations, never()).save(any());
    }

    @Test void onlyTheOwnerCanGrantAdmin() {
        User admin = user(3, "Admin");
        when(access.requireWorkspaceManager(3L, 10L)).thenReturn(ws);
        when(access.requireOrgMember(3L, 1L)).thenReturn(membership(admin, org, OrgRole.ADMIN));
        ApiException ex = assertThrows(ApiException.class, () -> service.invite(3L, 10L, new InviteRequest("x@example.com", OrgRole.ADMIN)));
        assertEquals(403, ex.getStatus().value());
    }

    @Test void ownerRoleCanNeverBeGrantedByInvite() {
        when(users.findByEmailIgnoreCase("x@example.com")).thenReturn(Optional.empty());
        when(invitations.findFirstByWorkspaceIdAndEmailIgnoreCaseAndStatus(any(), any(), any())).thenReturn(Optional.empty());
        InviteResult res = service.invite(1L, 10L, new InviteRequest("x@example.com", OrgRole.OWNER));
        assertEquals(OrgRole.MEMBER, res.invitation().role());
    }

    private Invitation invitation(String email, InviteStatus status, Instant expires) {
        Invitation i = new Invitation();
        i.setEmail(email); i.setToken("tok"); i.setStatus(status); i.setExpiresAt(expires); i.setOrganization(org); i.setWorkspace(ws); i.setRole(OrgRole.MEMBER);
        return i;
    }

    @Test void expiredRevokedOrUnknownTokensAreRejected() {
        when(invitations.findByToken("expired")).thenReturn(Optional.of(invitation("a@example.com", InviteStatus.PENDING, Instant.now().minusSeconds(5))));
        when(invitations.findByToken("revoked")).thenReturn(Optional.of(invitation("a@example.com", InviteStatus.REVOKED, Instant.now().plusSeconds(500))));
        when(invitations.findByToken("nope")).thenReturn(Optional.empty());
        for (String t : new String[]{"expired", "revoked", "nope"})
            assertEquals(400, assertThrows(ApiException.class, () -> service.requireUsable(t)).getStatus().value());
    }

    @Test void inviteMustBeAcceptedByTheInvitedEmail() {
        when(invitations.findByToken("tok")).thenReturn(Optional.of(invitation("bob@example.com", InviteStatus.PENDING, Instant.now().plusSeconds(500))));
        assertThrows(ApiException.class, () -> service.acceptForNewUser("tok", user(5, "Mallory")));
    }

    @Test void acceptingJoinsOrganizationAndWorkspaceAndConsumesTheInvite() {
        Invitation inv = invitation("bob@example.com", InviteStatus.PENDING, Instant.now().plusSeconds(500));
        when(invitations.findByToken("tok")).thenReturn(Optional.of(inv));
        when(memberships.findByUserIdAndOrganizationId(2L, 1L)).thenReturn(Optional.empty());
        when(workspaceMembers.existsByWorkspaceIdAndUserId(10L, 2L)).thenReturn(false);

        service.acceptForNewUser("tok", user(2, "Bob"));

        assertEquals(InviteStatus.ACCEPTED, inv.getStatus());
        verify(memberships).save(any(Membership.class));
        verify(workspaceMembers).save(any(WorkspaceMember.class));
        assertEquals(400, assertThrows(ApiException.class, () -> service.requireUsable("tok")).getStatus().value());
    }
}
