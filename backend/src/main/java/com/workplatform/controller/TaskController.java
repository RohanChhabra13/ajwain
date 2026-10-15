package com.workplatform.controller;

import com.workplatform.dto.Dtos.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

import com.workplatform.service.*;
import org.springframework.http.ResponseEntity;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {
    private final TaskService svc;
    private final AttachmentService attachments;

    @GetMapping("/{id}") public TaskResponse get(@AuthenticationPrincipal Long uid, @PathVariable Long id) { return svc.get(uid, id); }
    @PatchMapping("/{id}") public TaskResponse update(@AuthenticationPrincipal Long uid, @PathVariable Long id, @RequestBody UpdateTaskRequest r) { return svc.update(uid, id, r); }
    @DeleteMapping("/{id}") public ResponseEntity<Void> delete(@AuthenticationPrincipal Long uid, @PathVariable Long id) { svc.delete(uid, id); return ResponseEntity.noContent().build(); }
    @PostMapping("/{id}/subtasks") public TaskResponse createSubtask(@AuthenticationPrincipal Long uid, @PathVariable Long id, @Valid @RequestBody CreateTaskRequest r) { return svc.createSubtask(uid, id, r); }
    @GetMapping("/{id}/comments") public List<CommentResponse> comments(@AuthenticationPrincipal Long uid, @PathVariable Long id) { return svc.comments(uid, id); }
    @PostMapping("/{id}/comments") public CommentResponse addComment(@AuthenticationPrincipal Long uid, @PathVariable Long id, @Valid @RequestBody CommentRequest r) { return svc.addComment(uid, id, r); }
    @GetMapping("/{id}/activity") public List<ActivityResponse> activity(@AuthenticationPrincipal Long uid, @PathVariable Long id) { return svc.activity(uid, id); }
    @GetMapping("/{id}/attachments") public List<AttachmentResponse> attachments(@AuthenticationPrincipal Long uid, @PathVariable Long id) { return attachments.list(uid, id); }
    @PostMapping(value = "/{id}/attachments", consumes = "multipart/form-data")
    public AttachmentResponse upload(@AuthenticationPrincipal Long uid, @PathVariable Long id, @RequestParam("file") org.springframework.web.multipart.MultipartFile file) { return attachments.upload(uid, id, file); }
}
