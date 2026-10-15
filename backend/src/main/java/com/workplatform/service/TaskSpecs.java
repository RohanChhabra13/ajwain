package com.workplatform.service;

import com.workplatform.dto.Dtos.TaskFilter;
import com.workplatform.entity.*;
import com.workplatform.entity.Enums.*;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Builds the advanced-filter query. Always scoped to one workspace. */
public final class TaskSpecs {
    private TaskSpecs() {}

    public static Specification<Task> build(Long workspaceId, TaskFilter f, Long currentUserId, LocalDate today) {
        return (root, query, cb) -> {
            query.distinct(true);
            List<Predicate> p = new ArrayList<>();
            Join<Task, Project> project = root.join("project");
            Join<Task, Status> status = root.join("status");

            p.add(cb.equal(project.get("workspace").get("id"), workspaceId));
            if (!Boolean.TRUE.equals(f.includeArchived())) p.add(cb.equal(project.get("status"), ProjectStatus.ACTIVE));
            if (!Boolean.TRUE.equals(f.includeSubtasks())) p.add(cb.isNull(root.get("parent")));

            if (f.projectId() != null) p.add(cb.equal(project.get("id"), f.projectId()));
            if (f.epicId() != null) p.add(cb.equal(root.get("epic").get("id"), f.epicId()));
            if (f.statusId() != null) p.add(cb.equal(status.get("id"), f.statusId()));
            if (f.category() != null) p.add(cb.equal(status.get("category"), f.category()));
            if (f.priorityId() != null) p.add(cb.equal(root.get("priority").get("id"), f.priorityId()));

            String a = f.assignee();
            if (a != null && !a.isBlank()) {
                if (a.equalsIgnoreCase("me")) p.add(cb.equal(root.get("assignee").get("id"), currentUserId));
                else if (a.equalsIgnoreCase("none")) p.add(cb.isNull(root.get("assignee")));
                else {
                    try { p.add(cb.equal(root.get("assignee").get("id"), Long.valueOf(a))); }
                    catch (NumberFormatException e) { p.add(cb.disjunction()); }
                }
            }
            if (f.tag() != null && !f.tag().isBlank()) {
                Join<Task, Tag> tag = root.join("tags");
                p.add(cb.equal(cb.lower(tag.get("name")), f.tag().trim().toLowerCase()));
            }
            if (f.dueFrom() != null) p.add(cb.greaterThanOrEqualTo(root.<LocalDate>get("dueDate"), f.dueFrom()));
            if (f.dueTo() != null) p.add(cb.lessThanOrEqualTo(root.<LocalDate>get("dueDate"), f.dueTo()));
            if (Boolean.TRUE.equals(f.overdue())) {
                p.add(cb.lessThan(root.<LocalDate>get("dueDate"), today));
                p.add(cb.notEqual(status.get("category"), StatusCategory.DONE));
            }
            if (f.q() != null && !f.q().isBlank()) {
                String like = "%" + f.q().trim().toLowerCase().replace("%", "\\%").replace("_", "\\_") + "%";
                p.add(cb.or(cb.like(cb.lower(root.get("title")), like, '\\'),
                            cb.like(cb.lower(root.<String>get("description")), like, '\\')));
            }
            return cb.and(p.toArray(new Predicate[0]));
        };
    }
}
