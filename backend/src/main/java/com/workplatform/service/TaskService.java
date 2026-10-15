package com.workplatform.service;

import com.workplatform.dto.Dtos.*;
import com.workplatform.entity.*;
import com.workplatform.entity.Enums.*;
import com.workplatform.exception.ApiException;
import com.workplatform.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional
public class TaskService {
    private final AccessService access;
    private final WorkspaceConfigService config;
    private final TaskRepository tasks;
    private final TagRepository tags;
    private final UserRepository users;
    private final CommentRepository comments;
    private final ActivityRepository activities;
    private final CustomFieldValueRepository customValues;
    private final ActivityLogger logger;
    private final NotificationService notifications;
    private final AttachmentService attachments;
    private final TaskAssembler assembler;

    // ================= create =================
    public TaskResponse create(Long userId, Long projectId, CreateTaskRequest r) {
        Project p = access.requireProject(userId, projectId);
        return assembler.one(createIn(userId, p, null, r));
    }

    public TaskResponse createSubtask(Long userId, Long parentId, CreateTaskRequest r) {
        Task parent = access.requireTask(userId, parentId);
        if (parent.getParent() != null) throw ApiException.badRequest("Subtasks can't have their own subtasks");
        return assembler.one(createIn(userId, parent.getProject(), parent, r));
    }

    private Task createIn(Long userId, Project p, Task parent, CreateTaskRequest r) {
        Workspace ws = p.getWorkspace();
        Task t = new Task();
        t.setTitle(r.title().trim());
        t.setDescription(r.description());
        t.setStatus(r.statusId() != null ? config.requireStatus(ws.getId(), r.statusId()) : config.defaultStatus(ws.getId()));
        t.setPriority(r.priorityId() != null ? config.requirePriority(ws.getId(), r.priorityId()) : config.defaultPriority(ws.getId()));
        t.setDueDate(r.dueDate());
        t.setPlannedDate(r.plannedDate());
        t.setProject(p);
        t.setParent(parent);
        t.setCreatedBy(users.getReferenceById(userId));
        if (r.assigneeId() != null) t.setAssignee(requireAssignee(ws, r.assigneeId()));
        if (r.epicId() != null) t.setEpic(requireEpicIn(p, r.epicId()));
        else if (parent != null) t.setEpic(parent.getEpic());
        if (r.tags() != null) t.setTags(resolveTags(ws, r.tags()));
        if (parent == null && r.recurrence() != null) {
            t.setRecurrence(r.recurrence());
            t.setRecurrenceInterval(r.recurrenceInterval() == null ? 1 : Math.max(1, r.recurrenceInterval()));
        }
        if (t.getStatus().getCategory() == StatusCategory.DONE) t.setCompletedAt(Instant.now());
        tasks.save(t);
        config.applyValues(t, r.customFields());

        String kind = parent == null ? "task" : "subtask";
        logger.log(p, t, userId, "TASK_CREATED", "created " + kind + " \"" + t.getTitle() + "\"");
        if (t.getAssignee() != null) {
            logger.log(p, t, userId, "TASK_ASSIGNED", "assigned \"" + t.getTitle() + "\" to " + t.getAssignee().getName());
            notifications.notify(t.getAssignee().getId(), userId, "TASK_ASSIGNED", "You were assigned \"" + t.getTitle() + "\"", t);
        }
        return t;
    }

    // ================= read =================
    @Transactional(readOnly = true)
    public TaskResponse get(Long userId, Long taskId) { return assembler.one(access.requireTask(userId, taskId)); }

    // ================= update =================
    public TaskResponse update(Long userId, Long taskId, UpdateTaskRequest r) {
        Task t = access.requireTask(userId, taskId);
        Project p = t.getProject();
        Workspace ws = p.getWorkspace();

        if (r.title() != null && !r.title().isBlank() && !r.title().trim().equals(t.getTitle())) {
            logger.log(p, t, userId, "TASK_UPDATED", "renamed \"" + t.getTitle() + "\" to \"" + r.title().trim() + "\"");
            t.setTitle(r.title().trim());
        }
        if (r.description() != null) t.setDescription(r.description());

        if (r.statusId() != null && !r.statusId().equals(t.getStatus().getId())) changeStatus(userId, t, config.requireStatus(ws.getId(), r.statusId()));

        if (r.priorityId() != null && !r.priorityId().equals(t.getPriority().getId())) {
            Priority np = config.requirePriority(ws.getId(), r.priorityId());
            logger.log(p, t, userId, "TASK_PRIORITY_CHANGED", "set priority of \"" + t.getTitle() + "\" to " + np.getName());
            t.setPriority(np);
        }

        if (Boolean.TRUE.equals(r.clearDueDate())) {
            t.setDueDate(null);
        } else if (r.dueDate() != null && !r.dueDate().equals(t.getDueDate())) {
            logger.log(p, t, userId, "TASK_DUE_DATE_CHANGED", "set due date of \"" + t.getTitle() + "\" to " + r.dueDate());
            t.setDueDate(r.dueDate());
        }
        if (Boolean.TRUE.equals(r.clearPlannedDate())) t.setPlannedDate(null);
        else if (r.plannedDate() != null) t.setPlannedDate(r.plannedDate());

        if (Boolean.TRUE.equals(r.clearAssignee())) {
            t.setAssignee(null);
        } else if (r.assigneeId() != null && (t.getAssignee() == null || !t.getAssignee().getId().equals(r.assigneeId()))) {
            t.setAssignee(requireAssignee(ws, r.assigneeId()));
            logger.log(p, t, userId, "TASK_ASSIGNED", "assigned \"" + t.getTitle() + "\" to " + t.getAssignee().getName());
            notifications.notify(t.getAssignee().getId(), userId, "TASK_ASSIGNED", "You were assigned \"" + t.getTitle() + "\"", t);
        }

        if (Boolean.TRUE.equals(r.clearEpic())) t.setEpic(null);
        else if (r.epicId() != null) t.setEpic(requireEpicIn(p, r.epicId()));

        if (r.tags() != null) t.setTags(resolveTags(ws, r.tags()));
        if (t.getParent() == null && r.recurrence() != null) {
            t.setRecurrence(r.recurrence());
            if (r.recurrence() != Recurrence.NONE) t.setRecurrenceSpawned(false);
        }
        if (t.getParent() == null && r.recurrenceInterval() != null) t.setRecurrenceInterval(Math.max(1, r.recurrenceInterval()));
        config.applyValues(t, r.customFields());
        return assembler.one(t);
    }

    private void changeStatus(Long userId, Task t, Status next) {
        Status old = t.getStatus();
        logger.log(t.getProject(), t, userId, "TASK_STATUS_CHANGED",
                "moved \"" + t.getTitle() + "\" from " + old.getName() + " to " + next.getName());
        t.setStatus(next);
        boolean wasDone = old.getCategory() == StatusCategory.DONE;
        boolean isDone = next.getCategory() == StatusCategory.DONE;
        if (isDone && !wasDone) {
            t.setCompletedAt(Instant.now());
            spawnNextIfRecurring(userId, t);
        } else if (!isDone && wasDone) {
            t.setCompletedAt(null);
        }
        String msg = "\"" + t.getTitle() + "\" moved to " + next.getName();
        notifications.notify(t.getAssignee() == null ? null : t.getAssignee().getId(), userId, "TASK_STATUS_CHANGED", msg, t);
        if (t.getCreatedBy() != null && (t.getAssignee() == null || !t.getCreatedBy().getId().equals(t.getAssignee().getId())))
            notifications.notify(t.getCreatedBy().getId(), userId, "TASK_STATUS_CHANGED", msg, t);
    }

    /** Recurring tasks: completing one creates the next occurrence (once). */
    private void spawnNextIfRecurring(Long userId, Task t) {
        if (t.getRecurrence() == Recurrence.NONE || t.getParent() != null || t.isRecurrenceSpawned()) return;
        LocalDate today = LocalDate.now();
        LocalDate base = t.getDueDate() != null ? t.getDueDate() : today;
        Workspace ws = t.getProject().getWorkspace();

        Task n = new Task();
        n.setTitle(t.getTitle());
        n.setDescription(t.getDescription());
        n.setStatus(config.defaultStatus(ws.getId()));
        n.setPriority(t.getPriority());
        n.setDueDate(Recurrences.nextUpcoming(base, t.getRecurrence(), t.getRecurrenceInterval(), today));
        n.setProject(t.getProject());
        n.setEpic(t.getEpic());
        n.setAssignee(t.getAssignee());
        n.setCreatedBy(t.getCreatedBy());
        n.setTags(new HashSet<>(t.getTags()));
        n.setRecurrence(t.getRecurrence());
        n.setRecurrenceInterval(t.getRecurrenceInterval());
        tasks.save(n);
        for (CustomFieldValue v : customValues.findByTaskId(t.getId())) {
            CustomFieldValue copy = new CustomFieldValue();
            copy.setTask(n); copy.setField(v.getField()); copy.setValue(v.getValue());
            customValues.save(copy);
        }
        t.setRecurrenceSpawned(true);
        logger.log(t.getProject(), n, userId, "TASK_CREATED", "recurring task \"" + n.getTitle() + "\" scheduled for " + n.getDueDate());
    }

    // ================= delete =================
    public void delete(Long userId, Long taskId) {
        Task t = access.requireTask(userId, taskId);
        logger.log(t.getProject(), null, userId, "TASK_DELETED", "deleted task \"" + t.getTitle() + "\"");
        for (Task sub : tasks.findByParentIdOrderByCreatedAtAsc(taskId)) purge(sub);
        purge(t);
    }

    private void purge(Task t) {
        Long id = t.getId();
        comments.deleteByTaskId(id);
        customValues.deleteByTaskId(id);
        attachments.deleteAllFor(id);
        activities.detachTask(id);
        tasks.delete(t);
    }

    // ================= collaboration =================
    @Transactional(readOnly = true)
    public List<CommentResponse> comments(Long userId, Long taskId) {
        access.requireTask(userId, taskId);
        return comments.findByTaskIdOrderByCreatedAtAsc(taskId).stream().map(Mapper::comment).toList();
    }

    public CommentResponse addComment(Long userId, Long taskId, CommentRequest r) {
        Task t = access.requireTask(userId, taskId);
        Comment c = new Comment();
        c.setBody(r.body().trim()); c.setTask(t); c.setAuthor(users.getReferenceById(userId));
        comments.save(c);
        logger.log(t.getProject(), t, userId, "TASK_COMMENTED", "commented on \"" + t.getTitle() + "\"");
        String msg = users.getReferenceById(userId).getName() + " commented on \"" + t.getTitle() + "\"";
        Long assigneeId = t.getAssignee() == null ? null : t.getAssignee().getId();
        notifications.notify(assigneeId, userId, "TASK_COMMENTED", msg, t);
        Long creatorId = t.getCreatedBy().getId();
        if (!creatorId.equals(assigneeId)) notifications.notify(creatorId, userId, "TASK_COMMENTED", msg, t);
        return Mapper.comment(c);
    }

    @Transactional(readOnly = true)
    public List<ActivityResponse> activity(Long userId, Long taskId) {
        access.requireTask(userId, taskId);
        return activities.findByTaskIdOrderByCreatedAtDesc(taskId).stream().map(Mapper::activity).toList();
    }

    // ================= helpers =================
    private User requireAssignee(Workspace ws, Long assigneeId) {
        User u = users.findById(assigneeId).orElseThrow(() -> ApiException.badRequest("Assignee not found"));
        if (!access.canAccessWorkspace(assigneeId, ws)) throw ApiException.badRequest("Assignee must be a member of this workspace");
        return u;
    }

    private Epic requireEpicIn(Project p, Long epicId) {
        Epic e = access.requireEpicUnchecked(epicId);
        if (!e.getProject().getId().equals(p.getId())) throw ApiException.badRequest("Epic belongs to a different project");
        return e;
    }

    private Set<Tag> resolveTags(Workspace ws, List<String> names) {
        Set<Tag> result = new HashSet<>();
        for (String raw : names) {
            String name = raw == null ? "" : raw.trim();
            if (name.isEmpty()) continue;
            result.add(tags.findByWorkspaceIdAndNameIgnoreCase(ws.getId(), name).orElseGet(() -> {
                Tag tag = new Tag(); tag.setName(name); tag.setWorkspace(ws); return tags.save(tag);
            }));
        }
        return result;
    }
}
