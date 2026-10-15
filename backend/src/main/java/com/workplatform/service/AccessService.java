package com.workplatform.service;

import com.workplatform.entity.*;
import com.workplatform.entity.Enums.OrgRole;
import com.workplatform.exception.ApiException;
import com.workplatform.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Central tenant-isolation checks. Everything belongs to an organization; organization OWNER/ADMIN
 * can see every workspace in it, MEMBERs only the workspaces they were added to.
 */
@Service
@RequiredArgsConstructor
public class AccessService {
    private final MembershipRepository memberships;
    private final WorkspaceMemberRepository workspaceMembers;
    private final WorkspaceRepository workspaces;
    private final ProjectRepository projects;
    private final TaskRepository tasks;
    private final EpicRepository epics;

    public Membership requireOrgMember(Long userId, Long orgId) {
        return memberships.findByUserIdAndOrganizationId(userId, orgId).orElseThrow(ApiException::forbidden);
    }

    public static boolean isManager(Membership m) { return m.getRole() == OrgRole.OWNER || m.getRole() == OrgRole.ADMIN; }

    public boolean canAccessWorkspace(Long userId, Workspace ws) {
        return memberships.findByUserIdAndOrganizationId(userId, ws.getOrganization().getId())
                .map(m -> isManager(m) || workspaceMembers.existsByWorkspaceIdAndUserId(ws.getId(), userId))
                .orElse(false);
    }

    private Workspace loadWorkspace(Long workspaceId) {
        return workspaces.findById(workspaceId).orElseThrow(() -> ApiException.notFound("Workspace"));
    }

    public Workspace requireWorkspace(Long userId, Long workspaceId) {
        Workspace ws = loadWorkspace(workspaceId);
        if (!canAccessWorkspace(userId, ws)) throw ApiException.forbidden();
        return ws;
    }

    /** Owner/admin of the workspace's organization. */
    public Workspace requireWorkspaceManager(Long userId, Long workspaceId) {
        Workspace ws = loadWorkspace(workspaceId);
        Membership m = requireOrgMember(userId, ws.getOrganization().getId());
        if (!isManager(m)) throw ApiException.forbidden();
        return ws;
    }

    public Project requireProject(Long userId, Long projectId) {
        Project p = projects.findById(projectId).orElseThrow(() -> ApiException.notFound("Project"));
        if (!canAccessWorkspace(userId, p.getWorkspace())) throw ApiException.forbidden();
        return p;
    }

    public Task requireTask(Long userId, Long taskId) {
        Task t = tasks.findById(taskId).orElseThrow(() -> ApiException.notFound("Task"));
        if (!canAccessWorkspace(userId, t.getProject().getWorkspace())) throw ApiException.forbidden();
        return t;
    }

    public Epic requireEpic(Long userId, Long epicId) {
        Epic e = epics.findById(epicId).orElseThrow(() -> ApiException.notFound("Epic"));
        if (!canAccessWorkspace(userId, e.getProject().getWorkspace())) throw ApiException.forbidden();
        return e;
    }

    /** Plain lookup; the caller has already authorised access to the owning project. */
    public Epic requireEpicUnchecked(Long epicId) {
        return epics.findById(epicId).orElseThrow(() -> ApiException.badRequest("Epic not found"));
    }
}
