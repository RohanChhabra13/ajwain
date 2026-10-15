package com.workplatform.service;

import com.workplatform.dto.Dtos.*;
import com.workplatform.entity.*;
import com.workplatform.entity.Enums.*;
import com.workplatform.exception.ApiException;
import com.workplatform.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import static com.workplatform.service.TestData.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TaskServiceTest {
    @Mock AccessService access;
    @Mock WorkspaceConfigService config;
    @Mock TaskRepository tasks;
    @Mock TagRepository tags;
    @Mock UserRepository users;
    @Mock CommentRepository comments;
    @Mock ActivityRepository activities;
    @Mock CustomFieldValueRepository customValues;
    @Mock ActivityLogger logger;
    @Mock NotificationService notifications;
    @Mock AttachmentService attachments;
    @Mock TaskAssembler assembler;
    @InjectMocks TaskService service;

    Organization org = org(1);
    Workspace ws = workspace(10, org);
    Project project = project(100, ws);
    User alice = user(1, "Alice");
    User bob = user(2, "Bob");
    Status todo = status(1, ws, StatusCategory.TODO, "To do");
    Status doing = status(2, ws, StatusCategory.IN_PROGRESS, "In progress");
    Status done = status(3, ws, StatusCategory.DONE, "Done");
    Priority medium = priority(2, ws, 2, "Medium");

    @BeforeEach void setUp() {
        when(access.requireProject(1L, 100L)).thenReturn(project);
        when(users.getReferenceById(1L)).thenReturn(alice);
        when(users.getReferenceById(2L)).thenReturn(bob);
        when(config.defaultStatus(10L)).thenReturn(todo);
        when(config.defaultPriority(10L)).thenReturn(medium);
        when(config.requireStatus(10L, 1L)).thenReturn(todo);
        when(config.requireStatus(10L, 2L)).thenReturn(doing);
        when(config.requireStatus(10L, 3L)).thenReturn(done);
        when(tasks.save(any(Task.class))).thenAnswer(i -> i.getArgument(0));
    }

    private static CreateTaskRequest create(String title, Long assigneeId) {
        return new CreateTaskRequest(title, null, null, null, null, null, assigneeId, null, null, null, null, null);
    }

    private static UpdateTaskRequest moveTo(Long statusId) {
        return new UpdateTaskRequest(null, null, statusId, null, null, null, null, null, null, null, null, null, null, null, null, null);
    }

    private Task existing(long id) {
        Task t = task(id, project, todo, medium, alice);
        when(access.requireTask(1L, id)).thenReturn(t);
        return t;
    }

    // ---------- create ----------
    @Test void createUsesWorkspaceDefaultsAndRecordsCreator() {
        service.create(1L, 100L, create("  Write plan  ", null));
        ArgumentCaptor<Task> c = ArgumentCaptor.forClass(Task.class);
        verify(tasks).save(c.capture());
        Task t = c.getValue();
        assertEquals("Write plan", t.getTitle());
        assertSame(todo, t.getStatus());
        assertSame(medium, t.getPriority());
        assertSame(alice, t.getCreatedBy());
        assertNull(t.getCompletedAt());
        verify(logger).log(eq(project), any(Task.class), eq(1L), eq("TASK_CREATED"), anyString());
    }

    @Test void assigningNotifiesTheAssignee() {
        when(users.findById(2L)).thenReturn(Optional.of(bob));
        when(access.canAccessWorkspace(2L, ws)).thenReturn(true);
        service.create(1L, 100L, create("Review", 2L));
        verify(notifications).notify(eq(2L), eq(1L), eq("TASK_ASSIGNED"), anyString(), any(Task.class));
    }

    @Test void assigneeMustHaveAccessToTheWorkspace() {
        User outsider = user(9, "Outsider");
        when(users.findById(9L)).thenReturn(Optional.of(outsider));
        when(access.canAccessWorkspace(9L, ws)).thenReturn(false);
        ApiException ex = assertThrows(ApiException.class, () -> service.create(1L, 100L, create("Nope", 9L)));
        assertEquals(400, ex.getStatus().value());
        verify(tasks, never()).save(any());
    }

    @Test void subtasksCannotBeNested() {
        Task parent = existing(5);
        Task grand = task(4, project, todo, medium, alice);
        parent.setParent(grand);
        assertThrows(ApiException.class, () -> service.createSubtask(1L, 5L, create("Too deep", null)));
        verify(tasks, never()).save(any());
    }

    @Test void subtaskBelongsToParentsProjectAndInheritsItsEpic() {
        Task parent = existing(5);
        Epic epic = new Epic(); epic.setId(7L); epic.setProject(project);
        parent.setEpic(epic);
        service.createSubtask(1L, 5L, create("Child", null));
        ArgumentCaptor<Task> c = ArgumentCaptor.forClass(Task.class);
        verify(tasks).save(c.capture());
        assertSame(parent, c.getValue().getParent());
        assertSame(project, c.getValue().getProject());
        assertSame(epic, c.getValue().getEpic());
    }

    // ---------- status changes ----------
    @Test void movingToDoneStampsCompletionAndReopeningClearsIt() {
        Task t = existing(5);
        service.update(1L, 5L, moveTo(3L));
        assertSame(done, t.getStatus());
        assertNotNull(t.getCompletedAt());
        service.update(1L, 5L, moveTo(2L));
        assertSame(doing, t.getStatus());
        assertNull(t.getCompletedAt());
    }

    @Test void completingANonRecurringTaskSpawnsNothing() {
        existing(5);
        service.update(1L, 5L, moveTo(3L));
        verify(tasks, never()).save(any());
    }

    @Test void completingARecurringTaskCreatesTheNextOccurrenceExactlyOnce() {
        Task t = existing(5);
        t.setRecurrence(Recurrence.WEEKLY);
        t.setRecurrenceInterval(1);
        t.setDueDate(LocalDate.now().plusDays(1));
        t.setAssignee(bob);

        service.update(1L, 5L, moveTo(3L));

        ArgumentCaptor<Task> c = ArgumentCaptor.forClass(Task.class);
        verify(tasks, times(1)).save(c.capture());
        Task next = c.getValue();
        assertEquals(t.getTitle(), next.getTitle());
        assertSame(todo, next.getStatus());
        assertSame(bob, next.getAssignee());
        assertEquals(Recurrence.WEEKLY, next.getRecurrence());
        assertEquals(LocalDate.now().plusDays(8), next.getDueDate());
        assertTrue(t.isRecurrenceSpawned());

        // toggling done -> open -> done must not create duplicates
        service.update(1L, 5L, moveTo(1L));
        service.update(1L, 5L, moveTo(3L));
        verify(tasks, times(1)).save(any(Task.class));
    }

    @Test void statusChangeNotifiesAssigneeAndCreatorButNotTheActor() {
        Task t = existing(5);
        t.setAssignee(bob);                       // creator is Alice (the actor)
        service.update(1L, 5L, moveTo(2L));
        verify(notifications).notify(eq(2L), eq(1L), eq("TASK_STATUS_CHANGED"), anyString(), eq(t));
        verify(notifications).notify(eq(1L), eq(1L), eq("TASK_STATUS_CHANGED"), anyString(), eq(t)); // NotificationService itself skips self-notifications
    }

    @Test void unchangedStatusIsNotLogged() {
        existing(5);
        service.update(1L, 5L, moveTo(1L));
        verify(logger, never()).log(any(), any(), anyLong(), eq("TASK_STATUS_CHANGED"), anyString());
    }

    // ---------- comments ----------
    @Test void commentsNotifyAssigneeAndCreator() {
        Task t = existing(5);
        t.setAssignee(bob);
        User carol = user(3, "Carol");
        when(users.getReferenceById(3L)).thenReturn(carol);
        when(access.requireTask(3L, 5L)).thenReturn(t);

        CommentResponse res = service.addComment(3L, 5L, new CommentRequest("  Looks good  "));

        assertEquals("Looks good", res.body());
        verify(notifications).notify(eq(2L), eq(3L), eq("TASK_COMMENTED"), anyString(), eq(t));
        verify(notifications).notify(eq(1L), eq(3L), eq("TASK_COMMENTED"), anyString(), eq(t));
    }

    // ---------- delete ----------
    @Test void deletingATaskAlsoRemovesSubtasksCommentsAttachmentsAndFields() {
        Task t = existing(5);
        Task sub = task(6, project, todo, medium, alice);
        sub.setParent(t);
        when(tasks.findByParentIdOrderByCreatedAtAsc(5L)).thenReturn(List.of(sub));

        service.delete(1L, 5L);

        verify(attachments).deleteAllFor(6L);
        verify(attachments).deleteAllFor(5L);
        verify(comments).deleteByTaskId(5L);
        verify(comments).deleteByTaskId(6L);
        verify(customValues).deleteByTaskId(5L);
        verify(activities).detachTask(5L);
        verify(tasks).delete(sub);
        verify(tasks).delete(t);
    }
}
