package com.workplatform.service;

import com.workplatform.dto.Dtos.*;
import com.workplatform.entity.*;
import com.workplatform.entity.Enums.*;
import com.workplatform.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional
public class ProjectService {
    private final AccessService access;
    private final ProjectRepository projects;
    private final EpicRepository epics;
    private final TaskRepository tasks;
    private final ActivityLogger logger;

    @Transactional(readOnly = true)
    public ProjectResponse get(Long userId, Long projectId) { return Mapper.project(access.requireProject(userId, projectId)); }

    public ProjectResponse update(Long userId, Long projectId, ProjectRequest r) {
        Project p = access.requireProject(userId, projectId);
        p.setName(r.name().trim());
        p.setDescription(r.description());
        return Mapper.project(p);
    }

    public ProjectResponse setArchived(Long userId, Long projectId, boolean archived) {
        Project p = access.requireProject(userId, projectId);
        p.setStatus(archived ? ProjectStatus.ARCHIVED : ProjectStatus.ACTIVE);
        logger.log(p, null, userId, archived ? "PROJECT_ARCHIVED" : "PROJECT_RESTORED",
                (archived ? "archived" : "restored") + " project \"" + p.getName() + "\"");
        return Mapper.project(p);
    }

    // ---------- epics ----------
    @Transactional(readOnly = true)
    public List<EpicResponse> epics(Long userId, Long projectId) {
        access.requireProject(userId, projectId);
        Map<Long, long[]> counts = new HashMap<>();
        for (Object[] r : tasks.epicCounts(projectId, StatusCategory.DONE))
            counts.put((Long) r[0], new long[]{((Number) r[1]).longValue(), ((Number) r[2]).longValue()});
        return epics.findByProjectIdOrderByCreatedAtAsc(projectId).stream().map(e -> {
            long[] c = counts.getOrDefault(e.getId(), new long[]{0, 0});
            return toDto(e, c[0], c[1]);
        }).toList();
    }

    public EpicResponse createEpic(Long userId, Long projectId, EpicRequest r) {
        Project p = access.requireProject(userId, projectId);
        Epic e = new Epic();
        e.setName(r.name().trim()); e.setDescription(r.description()); e.setDueDate(r.dueDate()); e.setProject(p);
        epics.save(e);
        logger.log(p, null, userId, "EPIC_CREATED", "created epic \"" + e.getName() + "\"");
        return toDto(e, 0, 0);
    }

    public EpicResponse updateEpic(Long userId, Long epicId, EpicRequest r) {
        Epic e = access.requireEpic(userId, epicId);
        e.setName(r.name().trim()); e.setDescription(r.description()); e.setDueDate(r.dueDate());
        if (r.status() != null) e.setStatus(r.status());
        return toDto(e, 0, 0);
    }

    public void deleteEpic(Long userId, Long epicId) {
        Epic e = access.requireEpic(userId, epicId);
        tasks.clearEpic(epicId);
        epics.delete(e);
    }

    private EpicResponse toDto(Epic e, long total, long done) {
        return new EpicResponse(e.getId(), e.getName(), e.getDescription(), e.getStatus(), e.getDueDate(), e.getProject().getId(), total, done);
    }
}
