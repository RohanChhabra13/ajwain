package com.workplatform.service;

import com.workplatform.dto.Dtos.*;
import com.workplatform.entity.*;
import com.workplatform.entity.Enums.*;
import com.workplatform.exception.ApiException;
import com.workplatform.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional
public class WorkspaceService {
    private final AccessService access;
    private final MembershipRepository memberships;
    private final WorkspaceRepository workspaces;
    private final WorkspaceMemberRepository workspaceMembers;
    private final ProjectRepository projects;
    private final TaskRepository tasks;
    private final TagRepository tags;
    private final StatusRepository statuses;
    private final PriorityRepository priorities;
    private final CustomFieldRepository fields;
    private final WorkspaceConfigService config;
    private final ActivityLogger logger;

    @Transactional(readOnly = true)
    public List<WorkspaceResponse> listMine(Long userId) {
        List<Membership> mine = memberships.findByUserId(userId);
        Map<Long, Membership> byOrg = new HashMap<>();
        mine.forEach(m -> byOrg.put(m.getOrganization().getId(), m));
        Map<Long, Workspace> result = new TreeMap<>();
        List<Long> managedOrgs = mine.stream().filter(AccessService::isManager).map(m -> m.getOrganization().getId()).toList();
        if (!managedOrgs.isEmpty()) workspaces.findByOrganizationIdIn(managedOrgs).forEach(w -> result.put(w.getId(), w));
        workspaceMembers.findByUserId(userId).forEach(wm -> result.put(wm.getWorkspace().getId(), wm.getWorkspace()));
        return result.values().stream().map(w -> {
            Organization o = w.getOrganization();
            return new WorkspaceResponse(w.getId(), w.getName(), o.getId(), o.getName(), byOrg.get(o.getId()).getRole());
        }).toList();
    }

    public WorkspaceResponse create(Long userId, CreateWorkspaceRequest r) {
        Membership m = access.requireOrgMember(userId, r.organizationId());
        if (!AccessService.isManager(m)) throw ApiException.forbidden();
        Workspace w = new Workspace();
        w.setName(r.name().trim()); w.setOrganization(m.getOrganization());
        workspaces.save(w);
        config.seedDefaults(w);
        return new WorkspaceResponse(w.getId(), w.getName(), m.getOrganization().getId(), m.getOrganization().getName(), m.getRole());
    }

    public WorkspaceResponse rename(Long userId, Long workspaceId, String name) {
        Workspace w = access.requireWorkspaceManager(userId, workspaceId);
        w.setName(name.trim());
        Membership m = access.requireOrgMember(userId, w.getOrganization().getId());
        return new WorkspaceResponse(w.getId(), w.getName(), w.getOrganization().getId(), w.getOrganization().getName(), m.getRole());
    }

    // ---------- members ----------
    /** Everyone who can work in the workspace: org owners/admins plus members added to this workspace. */
    @Transactional(readOnly = true)
    public List<MemberResponse> members(Long userId, Long workspaceId) {
        Workspace ws = access.requireWorkspace(userId, workspaceId);
        Set<Long> added = new HashSet<>();
        workspaceMembers.findByWorkspaceId(workspaceId).forEach(wm -> added.add(wm.getUser().getId()));
        return memberships.findByOrganizationId(ws.getOrganization().getId()).stream()
                .filter(m -> AccessService.isManager(m) || added.contains(m.getUser().getId()))
                .sorted(Comparator.comparing((Membership m) -> m.getRole()).thenComparing(m -> m.getUser().getName(), String.CASE_INSENSITIVE_ORDER))
                .map(m -> new MemberResponse(m.getUser().getId(), m.getUser().getName(), m.getUser().getEmail(), m.getRole())).toList();
    }

    public MemberResponse changeRole(Long userId, Long workspaceId, Long targetUserId, OrgRole role) {
        Workspace ws = access.requireWorkspaceManager(userId, workspaceId);
        Membership actor = access.requireOrgMember(userId, ws.getOrganization().getId());
        if (actor.getRole() != OrgRole.OWNER) throw ApiException.forbidden();
        if (role == OrgRole.OWNER) throw ApiException.badRequest("There can only be one owner");
        Membership target = memberships.findByUserIdAndOrganizationId(targetUserId, ws.getOrganization().getId()).orElseThrow(() -> ApiException.notFound("Member"));
        if (target.getRole() == OrgRole.OWNER) throw ApiException.badRequest("The owner's role can't be changed");
        target.setRole(role);
        User u = target.getUser();
        return new MemberResponse(u.getId(), u.getName(), u.getEmail(), role);
    }

    /** Removes a MEMBER from the workspace and unassigns their tasks here. Admins must be demoted first. */
    public void removeMember(Long userId, Long workspaceId, Long targetUserId) {
        Workspace ws = access.requireWorkspaceManager(userId, workspaceId);
        Membership target = memberships.findByUserIdAndOrganizationId(targetUserId, ws.getOrganization().getId()).orElseThrow(() -> ApiException.notFound("Member"));
        if (AccessService.isManager(target)) throw ApiException.badRequest("Owners and admins have access to every workspace. Change their role first.");
        WorkspaceMember wm = workspaceMembers.findByWorkspaceIdAndUserId(workspaceId, targetUserId).orElseThrow(() -> ApiException.notFound("Member"));
        workspaceMembers.delete(wm);
        tasks.unassignInWorkspace(targetUserId, workspaceId);
    }

    // ---------- meta ----------
    @Transactional(readOnly = true)
    public MetaResponse meta(Long userId, Long workspaceId) {
        access.requireWorkspace(userId, workspaceId);
        return new MetaResponse(
                statuses.findByWorkspaceIdOrderByPositionAsc(workspaceId).stream().map(Mapper::status).toList(),
                priorities.findByWorkspaceIdOrderByLevelAsc(workspaceId).stream().map(Mapper::priority).toList(),
                members(userId, workspaceId),
                fields.findByWorkspaceIdOrderByPositionAsc(workspaceId).stream().map(Mapper::field).toList(),
                tags.findByWorkspaceIdOrderByNameAsc(workspaceId).stream().map(Tag::getName).toList());
    }

    // ---------- projects ----------
    @Transactional(readOnly = true)
    public List<ProjectResponse> projects(Long userId, Long workspaceId) {
        access.requireWorkspace(userId, workspaceId);
        return projects.findByWorkspaceIdOrderByCreatedAtDesc(workspaceId).stream().map(Mapper::project).toList();
    }

    public ProjectResponse createProject(Long userId, Long workspaceId, ProjectRequest r) {
        Workspace ws = access.requireWorkspace(userId, workspaceId);
        Project p = new Project();
        p.setName(r.name().trim()); p.setDescription(r.description()); p.setWorkspace(ws);
        projects.save(p);
        logger.log(p, null, userId, "PROJECT_CREATED", "created project \"" + p.getName() + "\"");
        return Mapper.project(p);
    }
}
