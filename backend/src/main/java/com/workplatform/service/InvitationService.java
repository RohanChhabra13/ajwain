package com.workplatform.service;

import com.workplatform.dto.Dtos.*;
import com.workplatform.entity.*;
import com.workplatform.entity.Enums.*;
import com.workplatform.exception.ApiException;
import com.workplatform.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

/**
 * Bringing people into a workspace. Existing users are added directly; people without an account get an
 * invitation link (no email sending in the MVP: the manager copies the link and shares it).
 */
@Service
@RequiredArgsConstructor
@Transactional
public class InvitationService {
    private static final SecureRandom RANDOM = new SecureRandom();

    private final AccessService access;
    private final UserRepository users;
    private final MembershipRepository memberships;
    private final WorkspaceMemberRepository workspaceMembers;
    private final InvitationRepository invitations;
    private final NotificationService notifications;

    @Value("${app.invitations.expiry-days:14}")
    private long expiryDays = 14;

    public InviteResult invite(Long actorId, Long workspaceId, InviteRequest r) {
        Workspace ws = access.requireWorkspaceManager(actorId, workspaceId);
        Organization org = ws.getOrganization();
        Membership actor = access.requireOrgMember(actorId, org.getId());
        OrgRole role = (r.role() == null || r.role() == OrgRole.OWNER) ? OrgRole.MEMBER : r.role();
        if (role == OrgRole.ADMIN && actor.getRole() != OrgRole.OWNER) throw ApiException.forbidden();
        String email = r.email().trim().toLowerCase();

        Optional<User> existing = users.findByEmailIgnoreCase(email);
        if (existing.isPresent()) {
            User u = existing.get();
            Membership m = memberships.findByUserIdAndOrganizationId(u.getId(), org.getId()).orElseGet(() -> addToOrg(u, org, role));
            if (!AccessService.isManager(m) && !workspaceMembers.existsByWorkspaceIdAndUserId(ws.getId(), u.getId())) {
                addToWorkspace(u, ws);
                notifications.notify(u.getId(), actorId, "ADDED_TO_WORKSPACE",
                        "You were added to " + org.getName() + " / " + ws.getName(), null);
            }
            return new InviteResult(false, new MemberResponse(u.getId(), u.getName(), u.getEmail(), m.getRole()), null);
        }

        Invitation inv = invitations.findFirstByWorkspaceIdAndEmailIgnoreCaseAndStatus(ws.getId(), email, InviteStatus.PENDING)
                .orElseGet(Invitation::new);
        inv.setEmail(email); inv.setRole(role); inv.setOrganization(org); inv.setWorkspace(ws);
        inv.setInvitedBy(users.getReferenceById(actorId));
        inv.setExpiresAt(Instant.now().plus(expiryDays, ChronoUnit.DAYS));
        if (inv.getToken() == null) inv.setToken(newToken());
        invitations.save(inv);
        return new InviteResult(true, null, toDto(inv));
    }

    @Transactional(readOnly = true)
    public List<InvitationResponse> pending(Long actorId, Long workspaceId) {
        access.requireWorkspaceManager(actorId, workspaceId);
        return invitations.findByWorkspaceIdAndStatusOrderByCreatedAtDesc(workspaceId, InviteStatus.PENDING).stream().map(this::toDto).toList();
    }

    public void revoke(Long actorId, Long invitationId) {
        Invitation inv = invitations.findById(invitationId).orElseThrow(() -> ApiException.notFound("Invitation"));
        access.requireWorkspaceManager(actorId, inv.getWorkspace().getId());
        inv.setStatus(InviteStatus.REVOKED);
    }

    /** Public: lets the sign-up page pre-fill the email and show where the person is being invited. */
    @Transactional(readOnly = true)
    public InvitationInfo info(String token) {
        Invitation inv = requireUsable(token);
        return new InvitationInfo(inv.getEmail(), inv.getOrganization().getName(), inv.getWorkspace().getName(), inv.getRole());
    }

    public Invitation requireUsable(String token) {
        Invitation inv = invitations.findByToken(token == null ? "" : token.trim())
                .filter(i -> i.getStatus() == InviteStatus.PENDING && i.getExpiresAt().isAfter(Instant.now()))
                .orElseThrow(() -> ApiException.badRequest("This invitation is invalid or has expired"));
        return inv;
    }

    /** Used at registration: validates the token against the new user's email and joins them to the workspace. */
    public Invitation acceptForNewUser(String token, User user) {
        Invitation inv = requireUsable(token);
        if (!inv.getEmail().equalsIgnoreCase(user.getEmail()))
            throw ApiException.badRequest("This invitation was issued to a different email address");
        join(inv, user);
        return inv;
    }

    /** For an already-registered user opening an invite link while logged in. */
    public InvitationInfo accept(Long userId, String token) {
        User user = users.findById(userId).orElseThrow(() -> ApiException.notFound("User"));
        Invitation inv = acceptForNewUser(token, user);
        return new InvitationInfo(inv.getEmail(), inv.getOrganization().getName(), inv.getWorkspace().getName(), inv.getRole());
    }

    private void join(Invitation inv, User user) {
        Organization org = inv.getOrganization();
        Membership m = memberships.findByUserIdAndOrganizationId(user.getId(), org.getId()).orElseGet(() -> addToOrg(user, org, inv.getRole()));
        if (!AccessService.isManager(m) && !workspaceMembers.existsByWorkspaceIdAndUserId(inv.getWorkspace().getId(), user.getId()))
            addToWorkspace(user, inv.getWorkspace());
        inv.setStatus(InviteStatus.ACCEPTED);
    }

    private Membership addToOrg(User u, Organization org, OrgRole role) {
        Membership m = new Membership();
        m.setUser(u); m.setOrganization(org); m.setRole(role);
        return memberships.save(m);
    }

    private void addToWorkspace(User u, Workspace ws) {
        WorkspaceMember wm = new WorkspaceMember();
        wm.setUser(u); wm.setWorkspace(ws);
        workspaceMembers.save(wm);
    }

    private InvitationResponse toDto(Invitation i) {
        return new InvitationResponse(i.getId(), i.getEmail(), i.getRole(), i.getToken(), i.getStatus(), i.getExpiresAt(),
                i.getWorkspace().getId(), i.getWorkspace().getName(), i.getOrganization().getName());
    }

    private static String newToken() {
        byte[] b = new byte[24];
        RANDOM.nextBytes(b);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(b);
    }
}
