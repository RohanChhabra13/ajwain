package com.workplatform.controller;

import com.workplatform.dto.Dtos.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

import com.workplatform.service.AuthService;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService auth;

    @PostMapping("/register") public AuthResponse register(@Valid @RequestBody RegisterRequest r) { return auth.register(r); }
    @PostMapping("/login") public AuthResponse login(@Valid @RequestBody LoginRequest r) { return auth.login(r); }
    @GetMapping("/me") public UserSummary me(@AuthenticationPrincipal Long uid) { return auth.me(uid); }
    @PutMapping("/me") public UserSummary updateProfile(@AuthenticationPrincipal Long uid, @Valid @RequestBody UpdateProfileRequest r) { return auth.updateProfile(uid, r); }
    @PostMapping("/change-password")
    public void changePassword(@AuthenticationPrincipal Long uid, @Valid @RequestBody ChangePasswordRequest r) { auth.changePassword(uid, r); }
    // Logout is stateless: the client discards its token.
}
