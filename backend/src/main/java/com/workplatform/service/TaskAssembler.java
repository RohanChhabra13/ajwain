package com.workplatform.service;

import com.workplatform.dto.Dtos.*;
import com.workplatform.entity.*;
import com.workplatform.entity.Enums.StatusCategory;
import com.workplatform.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.time.LocalDate;
import java.util.*;

/** Builds TaskResponses, batching the subtask and custom-field lookups so lists stay cheap. */
@Component
@RequiredArgsConstructor
public class TaskAssembler {
    private final TaskRepository tasks;
    private final CustomFieldValueRepository values;

    /** Detailed response including the subtask list. */
    public TaskResponse one(Task t) { return build(List.of(t), true).get(0); }

    /** List response (no subtask list, but with subtask counts). */
    public List<TaskResponse> many(Collection<Task> list) { return build(list, false); }

    private List<TaskResponse> build(Collection<Task> list, boolean withSubtasks) {
        if (list.isEmpty()) return List.of();
        List<Long> ids = list.stream().map(Task::getId).toList();

        Map<Long, long[]> counts = new HashMap<>();
        for (Object[] r : tasks.subtaskCounts(ids, StatusCategory.DONE))
            counts.put((Long) r[0], new long[]{((Number) r[1]).longValue(), ((Number) r[2]).longValue()});

        Map<Long, Map<Long, String>> custom = new HashMap<>();
        for (CustomFieldValue v : values.findByTaskIdIn(ids))
            custom.computeIfAbsent(v.getTask().getId(), k -> new HashMap<>()).put(v.getField().getId(), v.getValue());

        Map<Long, List<SubtaskSummary>> subs = new HashMap<>();
        if (withSubtasks) {
            for (Task s : tasks.findByParentIdIn(ids)) {
                subs.computeIfAbsent(s.getParent().getId(), k -> new ArrayList<>()).add(new SubtaskSummary(
                        s.getId(), s.getTitle(), s.getStatus().getId(), s.getStatus().getName(), s.getStatus().getCategory(), Mapper.user(s.getAssignee())));
            }
        }

        LocalDate today = LocalDate.now();
        List<TaskResponse> out = new ArrayList<>();
        for (Task t : list) {
            Status st = t.getStatus();
            Priority pr = t.getPriority();
            long[] c = counts.getOrDefault(t.getId(), new long[]{0, 0});
            boolean overdue = t.getDueDate() != null && t.getDueDate().isBefore(today) && st.getCategory() != StatusCategory.DONE;
            out.add(new TaskResponse(t.getId(), t.getTitle(), t.getDescription(),
                    st.getId(), st.getName(), st.getColor(), st.getCategory(),
                    pr.getId(), pr.getName(), pr.getLevel(), pr.getColor(),
                    t.getDueDate(), t.getPlannedDate(), overdue,
                    t.getProject().getId(), t.getProject().getName(),
                    t.getEpic() == null ? null : t.getEpic().getId(), t.getEpic() == null ? null : t.getEpic().getName(),
                    t.getParent() == null ? null : t.getParent().getId(),
                    Mapper.user(t.getAssignee()), Mapper.user(t.getCreatedBy()),
                    t.getTags().stream().map(Tag::getName).sorted().toList(),
                    t.getRecurrence(), t.getRecurrenceInterval(), custom.getOrDefault(t.getId(), Map.of()),
                    (int) c[0], (int) c[1], subs.getOrDefault(t.getId(), List.of()),
                    t.getCompletedAt(), t.getCreatedAt()));
        }
        return out;
    }
}
