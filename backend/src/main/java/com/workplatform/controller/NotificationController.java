package com.workplatform.controller;

import com.workplatform.dto.Dtos.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

import com.workplatform.service.NotificationService;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {
    private final NotificationService svc;

    @GetMapping public List<NotificationResponse> list(@AuthenticationPrincipal Long uid) { return svc.list(uid); }
    @GetMapping("/unread-count") public java.util.Map<String, Long> unread(@AuthenticationPrincipal Long uid) { return java.util.Map.of("count", svc.unreadCount(uid)); }
    @PostMapping("/{id}/read") public void read(@AuthenticationPrincipal Long uid, @PathVariable Long id) { svc.markRead(uid, id); }
    @PostMapping("/read-all") public void readAll(@AuthenticationPrincipal Long uid) { svc.markAllRead(uid); }
}
