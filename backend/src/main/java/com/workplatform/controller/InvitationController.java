package com.workplatform.controller;

import com.workplatform.dto.Dtos.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

import com.workplatform.service.InvitationService;

@RestController
@RequestMapping("/api/invitations")
@RequiredArgsConstructor
public class InvitationController {
    private final InvitationService invitations;

    /** Public (used by the sign-up page). */
    @GetMapping("/{token}") public InvitationInfo info(@PathVariable String token) { return invitations.info(token); }
    @PostMapping("/{token}/accept") public InvitationInfo accept(@AuthenticationPrincipal Long uid, @PathVariable String token) {
        return invitations.accept(uid, token);
    }
    @DeleteMapping("/{id:[0-9]+}") public void revoke(@AuthenticationPrincipal Long uid, @PathVariable Long id) { invitations.revoke(uid, id); }
}
