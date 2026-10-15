package com.workplatform.service;

import com.workplatform.dto.Dtos.*;
import com.workplatform.entity.*;
import com.workplatform.entity.Enums.*;
import com.workplatform.exception.ApiException;
import com.workplatform.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;

/** Basic reports computed in memory: fine for small-team data volumes. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportService {
    private static final int MAX_DAYS = 366;
    private final AccessService access;
    private final TaskRepository tasks;

    public ReportResponse report(Long userId, Long workspaceId, LocalDate from, LocalDate to) {
        access.requireWorkspace(userId, workspaceId);
        LocalDate today = LocalDate.now();
        LocalDate end = to == null ? today : to;
        LocalDate start = from == null ? end.minusDays(29) : from;
        if (start.isAfter(end)) throw ApiException.badRequest("'from' must not be after 'to'");
        if (Duration.between(start.atStartOfDay(), end.atStartOfDay()).toDays() > MAX_DAYS) throw ApiException.badRequest("Please choose a range of at most one year");

        List<Task> all = tasks.findByProjectWorkspaceId(workspaceId).stream()
                .filter(t -> t.getProject().getStatus() == ProjectStatus.ACTIVE).toList();

        Map<LocalDate, long[]> perDay = new TreeMap<>();
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) perDay.put(d, new long[2]);

        long created = 0, completed = 0, openOverdue = 0;
        double cycleSum = 0;
        Map<Long, MemberAgg> members = new LinkedHashMap<>();
        Map<Long, long[]> projects = new LinkedHashMap<>();
        Map<Long, String> projectNames = new HashMap<>();
        Map<String, Long> openByPriority = new LinkedHashMap<>();

        for (Task t : all) {
            boolean done = t.getStatus().getCategory() == StatusCategory.DONE;
            boolean overdue = !done && t.getDueDate() != null && t.getDueDate().isBefore(today);
            LocalDate createdDay = date(t.getCreatedAt());
            LocalDate doneDay = t.getCompletedAt() == null ? null : date(t.getCompletedAt());
            boolean completedInRange = done && doneDay != null && !doneDay.isBefore(start) && !doneDay.isAfter(end);

            if (createdDay != null && !createdDay.isBefore(start) && !createdDay.isAfter(end)) { created++; perDay.get(createdDay)[0]++; }
            if (completedInRange) {
                completed++; perDay.get(doneDay)[1]++;
                cycleSum += Duration.between(t.getCreatedAt(), t.getCompletedAt()).toHours() / 24.0;
            }
            if (overdue) openOverdue++;
            if (!done) openByPriority.merge(t.getPriority().getName(), 1L, Long::sum);

            long[] pr = projects.computeIfAbsent(t.getProject().getId(), k -> new long[2]);
            projectNames.put(t.getProject().getId(), t.getProject().getName());
            pr[0]++; if (done) pr[1]++;

            if (t.getAssignee() != null) {
                MemberAgg m = members.computeIfAbsent(t.getAssignee().getId(), k -> new MemberAgg(t.getAssignee().getName()));
                m.assigned++;
                if (completedInRange) m.completed++;
                if (overdue) m.overdue++;
            }
        }

        Double avg = completed == 0 ? null : Math.round(cycleSum / completed * 10.0) / 10.0;
        List<DayCount> daily = perDay.entrySet().stream().map(e -> new DayCount(e.getKey(), e.getValue()[0], e.getValue()[1])).toList();
        List<MemberReport> memberRows = members.entrySet().stream()
                .map(e -> new MemberReport(e.getKey(), e.getValue().name, e.getValue().assigned, e.getValue().completed, e.getValue().overdue)).toList();
        List<ProjectReport> projectRows = projects.entrySet().stream()
                .map(e -> new ProjectReport(e.getKey(), projectNames.get(e.getKey()), e.getValue()[0], e.getValue()[1])).toList();
        List<NameCount> prioRows = openByPriority.entrySet().stream().map(e -> new NameCount(e.getKey(), e.getValue())).toList();
        return new ReportResponse(start, end, created, completed, openOverdue, avg, daily, memberRows, projectRows, prioRows);
    }

    private static LocalDate date(Instant i) { return i == null ? null : i.atZone(ZoneOffset.UTC).toLocalDate(); }

    private static final class MemberAgg {
        final String name; long assigned, completed, overdue;
        MemberAgg(String name) { this.name = name; }
    }
}
