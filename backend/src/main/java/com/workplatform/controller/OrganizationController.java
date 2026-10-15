package com.workplatform.controller;

import com.workplatform.dto.Dtos.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

import com.workplatform.service.AuthService;

@RestController
@RequestMapping("/api/organizations")
@RequiredArgsConstructor
public class OrganizationController {
    private final AuthService auth;

    /** Starts a new organization owned by the caller, with a default workspace. */
    @PostMapping public WorkspaceResponse create(@AuthenticationPrincipal Long uid, @Valid @RequestBody CreateOrganizationRequest r) {
        return auth.createOrganizationFor(uid, r.name());
    }
}
