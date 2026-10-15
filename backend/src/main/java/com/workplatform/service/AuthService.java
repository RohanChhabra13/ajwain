package com.workplatform.service;

import com.workplatform.dto.Dtos.*;
import com.workplatform.entity.*;
import com.workplatform.entity.Enums.OrgRole;
import com.workplatform.exception.ApiException;
import com.workplatform.repository.*;
import com.workplatform.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository users;
    private final OrganizationRepository orgs;
    private final MembershipRepository memberships;
    private final WorkspaceRepository workspaces;
    private final WorkspaceConfigService config;
    private final InvitationService invitations;
    private final PasswordEncoder encoder;
    private final JwtService jwt;

    /**
     * Sign-up. Without an invite token the user gets their own organization + default workspace (self-serve setup).
     * With a valid invite token they join the inviting organization/workspace instead.
     */
    @Transactional
    public AuthResponse register(RegisterRequest r) {
        String email = r.email().trim().toLowerCase();
        if (users.existsByEmailIgnoreCase(email)) throw new ApiException(HttpStatus.CONFLICT, "Email already registered");
        boolean hasInvite = r.inviteToken() != null && !r.inviteToken().isBlank();
        if (hasInvite) invitations.requireUsable(r.inviteToken());   // fail fast before creating anything

        User u = new User();
        u.setName(r.name().trim());
        u.setEmail(email);
        u.setPasswordHash(encoder.encode(r.password()));
        users.save(u);

        if (hasInvite) {
            invitations.acceptForNewUser(r.inviteToken(), u);
        } else {
            createOrganization(u, (r.organizationName() == null || r.organizationName().isBlank()) ? u.getName() + "'s Team" : r.organizationName().trim());
        }
        return new AuthResponse(jwt.generate(u.getId()), Mapper.user(u));
    }

    /** Creates an organization owned by the user, with a default "General" workspace. */
    @Transactional
    public Workspace createOrganization(User owner, String name) {
        Organization org = new Organization();
        org.setName(name);
        orgs.save(org);
        Membership m = new Membership();
        m.setUser(owner); m.setOrganization(org); m.setRole(OrgRole.OWNER);
        memberships.save(m);
        Workspace ws = new Workspace();
        ws.setName("General"); ws.setOrganization(org);
        workspaces.save(ws);
        config.seedDefaults(ws);
        return ws;
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest r) {
        User u = users.findByEmailIgnoreCase(r.email().trim())
                .filter(x -> encoder.matches(r.password(), x.getPasswordHash()))
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));
        return new AuthResponse(jwt.generate(u.getId()), Mapper.user(u));
    }

    @Transactional(readOnly = true)
    public UserSummary me(Long userId) {
        return users.findById(userId).map(Mapper::user).orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Unknown user"));
    }

    @Transactional
    public UserSummary updateProfile(Long userId, UpdateProfileRequest r) {
        User u = users.findById(userId).orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Unknown user"));
        u.setName(r.name().trim());
        return Mapper.user(u);
    }

    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest r) {
        User u = users.findById(userId).orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Unknown user"));
        if (!encoder.matches(r.currentPassword(), u.getPasswordHash())) throw ApiException.badRequest("Current password is incorrect");
        u.setPasswordHash(encoder.encode(r.newPassword()));
    }

    /** Lets a signed-in user start an additional organization (with its own default workspace). */
    @Transactional
    public WorkspaceResponse createOrganizationFor(Long userId, String name) {
        User u = users.findById(userId).orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Unknown user"));
        Workspace ws = createOrganization(u, name.trim());
        return new WorkspaceResponse(ws.getId(), ws.getName(), ws.getOrganization().getId(), ws.getOrganization().getName(), OrgRole.OWNER);
    }
}
