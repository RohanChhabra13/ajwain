package com.workplatform.service;

import com.workplatform.dto.Dtos.*;
import com.workplatform.entity.*;
import com.workplatform.entity.Enums.OrgRole;
import com.workplatform.exception.ApiException;
import com.workplatform.repository.*;
import com.workplatform.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.Optional;
import static com.workplatform.service.TestData.user;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthServiceTest {
    @Mock UserRepository users;
    @Mock OrganizationRepository orgs;
    @Mock MembershipRepository memberships;
    @Mock WorkspaceRepository workspaces;
    @Mock WorkspaceConfigService config;
    @Mock InvitationService invitations;
    @Mock PasswordEncoder encoder;
    @Mock JwtService jwt;
    @InjectMocks AuthService auth;

    @Test void registerWithoutInviteCreatesOwnerOrganizationAndSeedsWorkspace() {
        when(users.existsByEmailIgnoreCase("new@example.com")).thenReturn(false);
        when(encoder.encode("password123")).thenReturn("hashed");
        when(jwt.generate(any())).thenReturn("token");

        AuthResponse res = auth.register(new RegisterRequest("New Person", "New@Example.com", "password123", "Acme", null));

        assertEquals("token", res.token());
        assertEquals("new@example.com", res.user().email());
        ArgumentCaptor<User> u = ArgumentCaptor.forClass(User.class);
        verify(users).save(u.capture());
        assertEquals("hashed", u.getValue().getPasswordHash());
        ArgumentCaptor<Organization> o = ArgumentCaptor.forClass(Organization.class);
        verify(orgs).save(o.capture());
        assertEquals("Acme", o.getValue().getName());
        ArgumentCaptor<Membership> m = ArgumentCaptor.forClass(Membership.class);
        verify(memberships).save(m.capture());
        assertEquals(OrgRole.OWNER, m.getValue().getRole());
        verify(config).seedDefaults(any(Workspace.class));
        verify(invitations, never()).acceptForNewUser(any(), any());
    }

    @Test void registerDefaultsOrganizationNameFromUserName() {
        when(encoder.encode(any())).thenReturn("h");
        auth.register(new RegisterRequest("Dana", "dana@example.com", "password123", " ", null));
        ArgumentCaptor<Organization> o = ArgumentCaptor.forClass(Organization.class);
        verify(orgs).save(o.capture());
        assertEquals("Dana's Team", o.getValue().getName());
    }

    @Test void registerWithInviteJoinsTheInvitingWorkspaceInsteadOfCreatingAnOrganization() {
        when(encoder.encode(any())).thenReturn("h");
        auth.register(new RegisterRequest("Bob", "bob@example.com", "password123", null, "tok123"));
        verify(invitations).requireUsable("tok123");
        verify(invitations).acceptForNewUser(eq("tok123"), any(User.class));
        verify(orgs, never()).save(any());
        verify(workspaces, never()).save(any());
    }

    @Test void invalidInviteFailsBeforeAnyUserIsCreated() {
        when(invitations.requireUsable("bad")).thenThrow(ApiException.badRequest("This invitation is invalid or has expired"));
        assertThrows(ApiException.class, () -> auth.register(new RegisterRequest("Bob", "bob@example.com", "password123", null, "bad")));
        verify(users, never()).save(any());
    }

    @Test void duplicateEmailIsRejectedWithConflict() {
        when(users.existsByEmailIgnoreCase("dup@example.com")).thenReturn(true);
        ApiException ex = assertThrows(ApiException.class, () -> auth.register(new RegisterRequest("X", "dup@example.com", "password123", null, null)));
        assertEquals(409, ex.getStatus().value());
        verify(users, never()).save(any());
    }

    @Test void loginChecksPassword() {
        User u = user(7, "Alice");
        when(users.findByEmailIgnoreCase("alice@example.com")).thenReturn(Optional.of(u));
        when(encoder.matches("right-password", "hash")).thenReturn(true);
        when(encoder.matches("wrong-password", "hash")).thenReturn(false);
        when(jwt.generate(7L)).thenReturn("tok");

        assertEquals("tok", auth.login(new LoginRequest("alice@example.com", "right-password")).token());
        assertEquals(401, assertThrows(ApiException.class, () -> auth.login(new LoginRequest("alice@example.com", "wrong-password"))).getStatus().value());
        when(users.findByEmailIgnoreCase("ghost@example.com")).thenReturn(Optional.empty());
        assertEquals(401, assertThrows(ApiException.class, () -> auth.login(new LoginRequest("ghost@example.com", "whatever12"))).getStatus().value());
    }

    @Test void changePasswordRequiresCurrentPassword() {
        User u = user(7, "Alice");
        when(users.findById(7L)).thenReturn(Optional.of(u));
        when(encoder.matches("old-password", "hash")).thenReturn(true);
        when(encoder.encode("new-password-1")).thenReturn("newhash");

        assertThrows(ApiException.class, () -> auth.changePassword(7L, new ChangePasswordRequest("nope", "new-password-1")));
        auth.changePassword(7L, new ChangePasswordRequest("old-password", "new-password-1"));
        assertEquals("newhash", u.getPasswordHash());
    }
}
