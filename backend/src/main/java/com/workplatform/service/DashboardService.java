package com.workplatform.service;

import com.workplatform.dto.Dtos.*;
import com.workplatform.entity.*;
import com.workplatform.entity.Enums.*;
import com.workplatform.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional
public class DashboardService {
    private static final ProjectStatus ACTIVE = ProjectStatus.ACTIVE;
    private static final StatusCategory DONE = StatusCategory.DONE;

    private final AccessService access;
    private final TaskRepository tasks;
    private final StatusRepository statuses;
    private final ActivityRepository activities;
    private final TaskAssembler assembler;

    @Transactional(readOnly = true)
    public List<TaskResponse> myWork(Long userId, Long workspaceId) {
        access.requireWorkspace(userId, workspaceId);
        return assembler.many(tasks.findMine(workspaceId, userId, ACTIVE));
    }

    @Transactional(readOnly = true)
    public DashboardResponse dashboard(Long userId, Long workspaceId) {
        access.requireWorkspace(userId, workspaceId);
        LocalDate today = LocalDate.now();

        Map<Long, Long> counts = new HashMap<>();
        for (Object[] row : tasks.countByStatus(workspaceId, ACTIVE)) counts.put((Long) row[0], ((Number) row[1]).longValue());
        List<StatusCount> statusCounts = statuses.findByWorkspaceIdOrderByPositionAsc(workspaceId).stream()
                .map(s -> new StatusCount(s.getId(), s.getName(), s.getColor(), s.getCategory(), counts.getOrDefault(s.getId(), 0L))).toList();

        Map<Long, long[]> perUser = new LinkedHashMap<>();
        Map<Long, String> names = new HashMap<>();
        for (Object[] row : tasks.countByAssignee(workspaceId, ACTIVE)) {
            Long uid = (Long) row[0];
            names.put(uid, (String) row[1]);
            perUser.computeIfAbsent(uid, k -> new long[3])[((StatusCategory) row[2]).ordinal()] = ((Number) row[3]).longValue();
        }
        List<MemberProgress> progress = perUser.entrySet().stream()
                .map(e -> new MemberProgress(e.getKey(), names.get(e.getKey()), e.getValue()[0], e.getValue()[1], e.getValue()[2])).toList();

        return new DashboardResponse(
                assembler.many(tasks.findDueOn(workspaceId, today, DONE, ACTIVE)),
                assembler.many(tasks.findOverdue(workspaceId, today, DONE, ACTIVE)),
                statusCounts, progress,
                activities.findTop30ByProjectWorkspaceIdOrderByCreatedAtDesc(workspaceId).stream().map(Mapper::activity).toList());
    }

    // ---------- daily planning ----------
    @Transactional(readOnly = true)
    public MyDayResponse myDay(Long userId, Long workspaceId) {
        access.requireWorkspace(userId, workspaceId);
        LocalDate today = LocalDate.now();
        List<Task> planned = tasks.findPlannedOn(workspaceId, userId, today);
        List<Task> carry = tasks.findCarryOver(workspaceId, userId, today, DONE, ACTIVE);
        Set<Long> carryIds = new HashSet<>();
        carry.forEach(t -> carryIds.add(t.getId()));
        List<Task> suggestions = tasks.findSuggestions(workspaceId, userId, today, DONE, ACTIVE).stream()
                .filter(t -> !carryIds.contains(t.getId())).toList();
        return new MyDayResponse(today, assembler.many(planned), assembler.many(carry), assembler.many(suggestions));
    }

    /** Moves everything planned for an earlier day and still unfinished onto today. Returns how many moved. */
    public int carryForward(Long userId, Long workspaceId) {
        access.requireWorkspace(userId, workspaceId);
        LocalDate today = LocalDate.now();
        List<Task> carry = tasks.findCarryOver(workspaceId, userId, today, DONE, ACTIVE);
        carry.forEach(t -> t.setPlannedDate(today));
        return carry.size();
    }
}
