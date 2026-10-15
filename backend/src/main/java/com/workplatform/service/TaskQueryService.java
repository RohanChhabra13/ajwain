package com.workplatform.service;

import com.workplatform.dto.Dtos.*;
import com.workplatform.entity.*;
import com.workplatform.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TaskQueryService {
    private static final int LIMIT = 500;
    private final AccessService access;
    private final TaskRepository tasks;
    private final TaskAssembler assembler;

    public List<Task> find(Long userId, Long workspaceId, TaskFilter f) {
        access.requireWorkspace(userId, workspaceId);
        List<Task> found = tasks.findAll(TaskSpecs.build(workspaceId, f, userId, LocalDate.now()), Sort.by(Sort.Direction.DESC, "createdAt"));
        return found.size() > LIMIT ? found.subList(0, LIMIT) : found;
    }

    public List<TaskResponse> search(Long userId, Long workspaceId, TaskFilter f) {
        return assembler.many(find(userId, workspaceId, f));
    }

    /** CSV export of the filtered tasks. Cells starting with = + - @ are prefixed to defuse spreadsheet formula injection. */
    public String exportCsv(Long userId, Long workspaceId, TaskFilter f) {
        StringBuilder sb = new StringBuilder("ID,Title,Project,Epic,Status,Priority,Assignee,Due date,Planned date,Completed,Tags,Parent ID\n");
        for (TaskResponse t : assembler.many(find(userId, workspaceId, f))) {
            sb.append(String.join(",",
                    String.valueOf(t.id()), csv(t.title()), csv(t.projectName()), csv(t.epicName()), csv(t.statusName()), csv(t.priorityName()),
                    csv(t.assignee() == null ? "" : t.assignee().name()), String.valueOf(t.dueDate() == null ? "" : t.dueDate()),
                    String.valueOf(t.plannedDate() == null ? "" : t.plannedDate()), String.valueOf(t.completedAt() == null ? "" : t.completedAt()),
                    csv(String.join("; ", t.tags())), String.valueOf(t.parentId() == null ? "" : t.parentId()))).append('\n');
        }
        return sb.toString();
    }

    static String csv(String v) {
        if (v == null) return "";
        String s = v;
        if (!s.isEmpty() && "=+-@\t\r".indexOf(s.charAt(0)) >= 0) s = "'" + s;
        return "\"" + s.replace("\"", "\"\"") + "\"";
    }
}
