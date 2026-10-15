package com.workplatform.controller;

import com.workplatform.dto.Dtos.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

import com.workplatform.entity.Enums.*;
import com.workplatform.service.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/workspaces")
@RequiredArgsConstructor
public class WorkspaceController {
    private final WorkspaceService svc;
    private final InvitationService invitations;
    private final DashboardService dashboard;
    private final TaskQueryService queries;
    private final ReportService reports;

    @GetMapping public List<WorkspaceResponse> list(@AuthenticationPrincipal Long uid) { return svc.listMine(uid); }
    @PostMapping public WorkspaceResponse create(@AuthenticationPrincipal Long uid, @Valid @RequestBody CreateWorkspaceRequest r) { return svc.create(uid, r); }
    @PatchMapping("/{id}") public WorkspaceResponse rename(@AuthenticationPrincipal Long uid, @PathVariable Long id, @Valid @RequestBody RenameRequest r) { return svc.rename(uid, id, r.name()); }
    @GetMapping("/{id}/meta") public MetaResponse meta(@AuthenticationPrincipal Long uid, @PathVariable Long id) { return svc.meta(uid, id); }

    // ----- members & invitations -----
    @GetMapping("/{id}/members") public List<MemberResponse> members(@AuthenticationPrincipal Long uid, @PathVariable Long id) { return svc.members(uid, id); }
    @PatchMapping("/{id}/members/{userId}") public MemberResponse changeRole(@AuthenticationPrincipal Long uid, @PathVariable Long id, @PathVariable Long userId, @Valid @RequestBody ChangeRoleRequest r) { return svc.changeRole(uid, id, userId, r.role()); }
    @DeleteMapping("/{id}/members/{userId}") public void removeMember(@AuthenticationPrincipal Long uid, @PathVariable Long id, @PathVariable Long userId) { svc.removeMember(uid, id, userId); }
    @PostMapping("/{id}/invitations") public InviteResult invite(@AuthenticationPrincipal Long uid, @PathVariable Long id, @Valid @RequestBody InviteRequest r) { return invitations.invite(uid, id, r); }
    @GetMapping("/{id}/invitations") public List<InvitationResponse> pendingInvitations(@AuthenticationPrincipal Long uid, @PathVariable Long id) { return invitations.pending(uid, id); }

    // ----- projects -----
    @GetMapping("/{id}/projects") public List<ProjectResponse> projects(@AuthenticationPrincipal Long uid, @PathVariable Long id) { return svc.projects(uid, id); }
    @PostMapping("/{id}/projects") public ProjectResponse createProject(@AuthenticationPrincipal Long uid, @PathVariable Long id, @Valid @RequestBody ProjectRequest r) { return svc.createProject(uid, id, r); }

    // ----- views -----
    @GetMapping("/{id}/my-work") public List<TaskResponse> myWork(@AuthenticationPrincipal Long uid, @PathVariable Long id) { return dashboard.myWork(uid, id); }
    @GetMapping("/{id}/my-day") public MyDayResponse myDay(@AuthenticationPrincipal Long uid, @PathVariable Long id) { return dashboard.myDay(uid, id); }
    @PostMapping("/{id}/my-day/carry-forward") public java.util.Map<String, Integer> carryForward(@AuthenticationPrincipal Long uid, @PathVariable Long id) { return java.util.Map.of("moved", dashboard.carryForward(uid, id)); }
    @GetMapping("/{id}/dashboard") public DashboardResponse dashboard(@AuthenticationPrincipal Long uid, @PathVariable Long id) { return dashboard.dashboard(uid, id); }

    // ----- advanced filtering / export -----
    @GetMapping("/{id}/tasks")
    public List<TaskResponse> search(@AuthenticationPrincipal Long uid, @PathVariable Long id,
            @RequestParam(required = false) String q, @RequestParam(required = false) Long projectId, @RequestParam(required = false) Long epicId,
            @RequestParam(required = false) Long statusId, @RequestParam(required = false) StatusCategory category,
            @RequestParam(required = false) Long priorityId, @RequestParam(required = false) String assignee, @RequestParam(required = false) String tag,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueTo,
            @RequestParam(required = false) Boolean overdue, @RequestParam(required = false) Boolean includeSubtasks,
            @RequestParam(required = false) Boolean includeArchived) {
        return queries.search(uid, id, new TaskFilter(q, projectId, epicId, statusId, category, priorityId, assignee, tag, dueFrom, dueTo, overdue, includeSubtasks, includeArchived));
    }

    @GetMapping("/{id}/tasks/export.csv")
    public ResponseEntity<String> exportCsv(@AuthenticationPrincipal Long uid, @PathVariable Long id,
            @RequestParam(required = false) Long projectId, @RequestParam(required = false) Boolean includeArchived) {
        String csv = queries.exportCsv(uid, id, new TaskFilter(null, projectId, null, null, null, null, null, null, null, null, null, true, includeArchived));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"tasks.csv\"")
                .contentType(new MediaType("text", "csv", java.nio.charset.StandardCharsets.UTF_8))
                .body(csv);
    }

    @GetMapping("/{id}/reports")
    public ReportResponse report(@AuthenticationPrincipal Long uid, @PathVariable Long id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return reports.report(uid, id, from, to);
    }
}
