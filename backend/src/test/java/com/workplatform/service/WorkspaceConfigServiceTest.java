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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import static com.workplatform.service.TestData.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WorkspaceConfigServiceTest {
    @Mock AccessService access;
    @Mock StatusRepository statuses;
    @Mock PriorityRepository priorities;
    @Mock CustomFieldRepository fields;
    @Mock CustomFieldValueRepository values;
    @Mock TaskRepository tasks;
    @InjectMocks WorkspaceConfigService service;

    Workspace ws = workspace(10, org(1));
    Status todo = status(1, ws, StatusCategory.TODO, "To do");
    Status review = status(2, ws, StatusCategory.IN_PROGRESS, "In review");
    Status done = status(3, ws, StatusCategory.DONE, "Done");

    @BeforeEach void setUp() {
        when(statuses.findById(1L)).thenReturn(Optional.of(todo));
        when(statuses.findById(2L)).thenReturn(Optional.of(review));
        when(statuses.findById(3L)).thenReturn(Optional.of(done));
    }

    @Test void seedDefaultsCreatesFourStatusesAndFourPriorities() {
        service.seedDefaults(ws);
        ArgumentCaptor<Status> s = ArgumentCaptor.forClass(Status.class);
        verify(statuses, times(4)).save(s.capture());
        assertEquals(List.of("To do", "In progress", "In review", "Done"), s.getAllValues().stream().map(Status::getName).toList());
        assertEquals(StatusCategory.DONE, s.getAllValues().get(3).getCategory());
        verify(priorities, times(4)).save(any(Priority.class));
    }

    @Test void statusInUseCannotBeDeleted() {
        when(tasks.countByStatusId(2L)).thenReturn(3L);
        assertThrows(ApiException.class, () -> service.deleteStatus(1L, 10L, 2L));
        verify(statuses, never()).delete(any());
    }

    @Test void lastDoneOrTodoStatusCannotBeDeleted() {
        when(tasks.countByStatusId(anyLong())).thenReturn(0L);
        when(statuses.countByWorkspaceIdAndCategory(10L, StatusCategory.DONE)).thenReturn(1L);
        when(statuses.countByWorkspaceIdAndCategory(10L, StatusCategory.TODO)).thenReturn(1L);
        assertThrows(ApiException.class, () -> service.deleteStatus(1L, 10L, 3L));
        assertThrows(ApiException.class, () -> service.deleteStatus(1L, 10L, 1L));
        verify(statuses, never()).delete(any());
    }

    @Test void unusedCustomStatusCanBeDeleted() {
        when(tasks.countByStatusId(2L)).thenReturn(0L);
        service.deleteStatus(1L, 10L, 2L);
        verify(statuses).delete(review);
    }

    @Test void statusFromAnotherWorkspaceIsRejected() {
        Status foreign = status(50, workspace(99, org(2)), StatusCategory.TODO, "Other");
        when(statuses.findById(50L)).thenReturn(Optional.of(foreign));
        assertThrows(ApiException.class, () -> service.requireStatus(10L, 50L));
    }

    @Test void defaultsPickFirstTodoStatusAndTheMiddlePriority() {
        when(statuses.findByWorkspaceIdOrderByPositionAsc(10L)).thenReturn(List.of(review, todo, done));
        assertSame(todo, service.defaultStatus(10L));
        List<Priority> ps = List.of(priority(1, ws, 1, "Low"), priority(2, ws, 2, "Medium"), priority(3, ws, 3, "High"), priority(4, ws, 4, "Urgent"));
        when(priorities.findByWorkspaceIdOrderByLevelAsc(10L)).thenReturn(ps);
        assertEquals("Medium", service.defaultPriority(10L).getName());
    }

    // ---------- custom fields ----------
    private CustomField field(long id, FieldType type, String options) {
        CustomField f = new CustomField();
        f.setId(id); f.setName("Field " + id); f.setType(type); f.setOptions(options); f.setWorkspace(ws);
        when(fields.findById(id)).thenReturn(Optional.of(f));
        return f;
    }

    private Task savedTask() {
        Task t = task(5, project(100, ws), todo, priority(2, ws, 2, "Medium"), user(1, "Alice"));
        return t;
    }

    @Test void numberFieldsMustBeNumeric() {
        field(1, FieldType.NUMBER, null);
        Task t = savedTask();
        assertThrows(ApiException.class, () -> service.applyValues(t, Map.of(1L, "abc")));
        service.applyValues(t, Map.of(1L, "12.5"));
        ArgumentCaptor<CustomFieldValue> c = ArgumentCaptor.forClass(CustomFieldValue.class);
        verify(values).save(c.capture());
        assertEquals("12.5", c.getValue().getValue());
    }

    @Test void dateFieldsMustBeIsoDates() {
        field(2, FieldType.DATE, null);
        assertThrows(ApiException.class, () -> service.applyValues(savedTask(), Map.of(2L, "31/12/2026")));
        service.applyValues(savedTask(), Map.of(2L, "2026-12-31"));
        verify(values).save(any(CustomFieldValue.class));
    }

    @Test void selectFieldsMustUseAConfiguredOption() {
        field(3, FieldType.SELECT, "Small\nLarge");
        assertThrows(ApiException.class, () -> service.applyValues(savedTask(), Map.of(3L, "Huge")));
        service.applyValues(savedTask(), Map.of(3L, "Large"));
        verify(values).save(any(CustomFieldValue.class));
    }

    @Test void blankValueClearsAnExistingFieldValue() {
        field(1, FieldType.TEXT, null);
        CustomFieldValue existing = new CustomFieldValue();
        when(values.findByTaskIdAndFieldId(5L, 1L)).thenReturn(Optional.of(existing));
        service.applyValues(savedTask(), Map.of(1L, "  "));
        verify(values).delete(existing);
        verify(values, never()).save(any());
    }

    @Test void fieldsFromAnotherWorkspaceAreRejected() {
        CustomField foreign = new CustomField();
        foreign.setId(77L); foreign.setType(FieldType.TEXT); foreign.setWorkspace(workspace(99, org(2)));
        when(fields.findById(77L)).thenReturn(Optional.of(foreign));
        assertThrows(ApiException.class, () -> service.applyValues(savedTask(), Map.of(77L, "x")));
    }

    @Test void selectFieldRequiresOptions() {
        assertThrows(ApiException.class, () -> service.createField(1L, 10L, new CustomFieldRequest("Size", FieldType.SELECT, List.of(" "))));
    }
}
