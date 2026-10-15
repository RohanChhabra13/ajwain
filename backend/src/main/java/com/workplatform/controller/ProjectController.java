package com.workplatform.controller;

import com.workplatform.dto.Dtos.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

import com.workplatform.service.*;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectController {
    private final ProjectService projects;
    private final TaskService tasks;

    @GetMapping("/{id}") public ProjectResponse get(@AuthenticationPrincipal Long uid, @PathVariable Long id) { return projects.get(uid, id); }
    @PutMapping("/{id}") public ProjectResponse update(@AuthenticationPrincipal Long uid, @PathVariable Long id, @Valid @RequestBody ProjectRequest r) { return projects.update(uid, id, r); }
    @PostMapping("/{id}/archive") public ProjectResponse archive(@AuthenticationPrincipal Long uid, @PathVariable Long id) { return projects.setArchived(uid, id, true); }
    @PostMapping("/{id}/restore") public ProjectResponse restore(@AuthenticationPrincipal Long uid, @PathVariable Long id) { return projects.setArchived(uid, id, false); }
    @PostMapping("/{id}/tasks") public TaskResponse createTask(@AuthenticationPrincipal Long uid, @PathVariable Long id, @Valid @RequestBody CreateTaskRequest r) { return tasks.create(uid, id, r); }
    @GetMapping("/{id}/epics") public List<EpicResponse> epics(@AuthenticationPrincipal Long uid, @PathVariable Long id) { return projects.epics(uid, id); }
    @PostMapping("/{id}/epics") public EpicResponse createEpic(@AuthenticationPrincipal Long uid, @PathVariable Long id, @Valid @RequestBody EpicRequest r) { return projects.createEpic(uid, id, r); }
}
