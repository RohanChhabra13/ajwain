package com.workplatform.service;

import com.workplatform.dto.Dtos.*;
import com.workplatform.entity.*;
import com.workplatform.entity.Enums.*;
import com.workplatform.exception.ApiException;
import com.workplatform.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional
public class WorkspaceConfigService {
    private final AccessService access;
    private final StatusRepository statuses;
    private final PriorityRepository priorities;
    private final CustomFieldRepository fields;
    private final CustomFieldValueRepository values;
    private final TaskRepository tasks;

    // ---------- defaults ----------
    public void seedDefaults(Workspace ws) {
        String[][] st = {{"To do", "#868e96", "TODO"}, {"In progress", "#3b5bdb", "IN_PROGRESS"},
                {"In review", "#f08c00", "IN_PROGRESS"}, {"Done", "#2b8a3e", "DONE"}};
        for (int i = 0; i < st.length; i++) {
            Status s = new Status();
            s.setName(st[i][0]); s.setColor(st[i][1]); s.setCategory(StatusCategory.valueOf(st[i][2]));
            s.setPosition(i); s.setWorkspace(ws);
            statuses.save(s);
        }
        String[][] pr = {{"Low", "1", "#868e96"}, {"Medium", "2", "#3b5bdb"}, {"High", "3", "#f08c00"}, {"Urgent", "4", "#c92a2a"}};
        for (String[] p : pr) {
            Priority x = new Priority();
            x.setName(p[0]); x.setLevel(Integer.parseInt(p[1])); x.setColor(p[2]); x.setWorkspace(ws);
            priorities.save(x);
        }
    }

    // ---------- lookups used by other services ----------
    public Status requireStatus(Long wsId, Long id) {
        return statuses.findById(id).filter(s -> s.getWorkspace().getId().equals(wsId))
                .orElseThrow(() -> ApiException.badRequest("Unknown status"));
    }

    public Priority requirePriority(Long wsId, Long id) {
        return priorities.findById(id).filter(p -> p.getWorkspace().getId().equals(wsId))
                .orElseThrow(() -> ApiException.badRequest("Unknown priority"));
    }

    public Status defaultStatus(Long wsId) {
        List<Status> all = statuses.findByWorkspaceIdOrderByPositionAsc(wsId);
        if (all.isEmpty()) throw ApiException.badRequest("Workspace has no statuses");
        return all.stream().filter(s -> s.getCategory() == StatusCategory.TODO).findFirst().orElse(all.get(0));
    }

    public Priority defaultPriority(Long wsId) {
        List<Priority> all = priorities.findByWorkspaceIdOrderByLevelAsc(wsId);
        if (all.isEmpty()) throw ApiException.badRequest("Workspace has no priorities");
        return all.get((all.size() - 1) / 2);
    }

    // ---------- statuses ----------
    @Transactional(readOnly = true)
    public List<StatusDto> listStatuses(Long uid, Long wsId) {
        access.requireWorkspace(uid, wsId);
        return statuses.findByWorkspaceIdOrderByPositionAsc(wsId).stream().map(Mapper::status).toList();
    }

    public StatusDto createStatus(Long uid, Long wsId, StatusRequest r) {
        Workspace ws = access.requireWorkspaceManager(uid, wsId);
        Status s = new Status();
        s.setName(r.name().trim()); s.setColor(r.color()); s.setCategory(r.category()); s.setWorkspace(ws);
        s.setPosition(r.position() != null ? r.position() : statuses.findByWorkspaceIdOrderByPositionAsc(wsId).size());
        return Mapper.status(statuses.save(s));
    }

    public StatusDto updateStatus(Long uid, Long wsId, Long id, StatusRequest r) {
        access.requireWorkspaceManager(uid, wsId);
        Status s = requireStatus(wsId, id);
        if (s.getCategory() != r.category()) requireCategoryStaysCovered(wsId, s);
        s.setName(r.name().trim()); s.setColor(r.color()); s.setCategory(r.category());
        if (r.position() != null) s.setPosition(r.position());
        return Mapper.status(s);
    }

    public void deleteStatus(Long uid, Long wsId, Long id) {
        access.requireWorkspaceManager(uid, wsId);
        Status s = requireStatus(wsId, id);
        if (tasks.countByStatusId(id) > 0) throw ApiException.badRequest("Move the tasks out of this status before deleting it");
        requireCategoryStaysCovered(wsId, s);
        statuses.delete(s);
    }

    /** A workspace always needs at least one TODO and one DONE status for the workflow (and recurrence) to work. */
    private void requireCategoryStaysCovered(Long wsId, Status leaving) {
        StatusCategory c = leaving.getCategory();
        if ((c == StatusCategory.TODO || c == StatusCategory.DONE) && statuses.countByWorkspaceIdAndCategory(wsId, c) <= 1)
            throw ApiException.badRequest("Keep at least one status in the " + c + " category");
    }

    // ---------- priorities ----------
    @Transactional(readOnly = true)
    public List<PriorityDto> listPriorities(Long uid, Long wsId) {
        access.requireWorkspace(uid, wsId);
        return priorities.findByWorkspaceIdOrderByLevelAsc(wsId).stream().map(Mapper::priority).toList();
    }

    public PriorityDto createPriority(Long uid, Long wsId, PriorityRequest r) {
        Workspace ws = access.requireWorkspaceManager(uid, wsId);
        Priority p = new Priority();
        p.setName(r.name().trim()); p.setLevel(r.level()); p.setColor(r.color()); p.setWorkspace(ws);
        return Mapper.priority(priorities.save(p));
    }

    public PriorityDto updatePriority(Long uid, Long wsId, Long id, PriorityRequest r) {
        access.requireWorkspaceManager(uid, wsId);
        Priority p = requirePriority(wsId, id);
        p.setName(r.name().trim()); p.setLevel(r.level()); p.setColor(r.color());
        return Mapper.priority(p);
    }

    public void deletePriority(Long uid, Long wsId, Long id) {
        access.requireWorkspaceManager(uid, wsId);
        Priority p = requirePriority(wsId, id);
        if (tasks.countByPriorityId(id) > 0) throw ApiException.badRequest("Change the priority of those tasks before deleting it");
        if (priorities.findByWorkspaceIdOrderByLevelAsc(wsId).size() <= 1) throw ApiException.badRequest("Keep at least one priority");
        priorities.delete(p);
    }

    // ---------- custom fields ----------
    @Transactional(readOnly = true)
    public List<CustomFieldDto> listFields(Long uid, Long wsId) {
        access.requireWorkspace(uid, wsId);
        return fields.findByWorkspaceIdOrderByPositionAsc(wsId).stream().map(Mapper::field).toList();
    }

    public CustomFieldDto createField(Long uid, Long wsId, CustomFieldRequest r) {
        Workspace ws = access.requireWorkspaceManager(uid, wsId);
        CustomField f = new CustomField();
        apply(f, r);
        f.setWorkspace(ws);
        f.setPosition(fields.findByWorkspaceIdOrderByPositionAsc(wsId).size());
        return Mapper.field(fields.save(f));
    }

    public CustomFieldDto updateField(Long uid, Long wsId, Long id, CustomFieldRequest r) {
        access.requireWorkspaceManager(uid, wsId);
        CustomField f = requireField(wsId, id);
        apply(f, r);
        return Mapper.field(f);
    }

    public void deleteField(Long uid, Long wsId, Long id) {
        access.requireWorkspaceManager(uid, wsId);
        CustomField f = requireField(wsId, id);
        values.deleteByFieldId(id);
        fields.delete(f);
    }

    private void apply(CustomField f, CustomFieldRequest r) {
        f.setName(r.name().trim());
        f.setType(r.type());
        List<String> opts = r.options() == null ? List.of() : r.options().stream().map(String::trim).filter(s -> !s.isEmpty()).distinct().toList();
        if (r.type() == FieldType.SELECT && opts.isEmpty()) throw ApiException.badRequest("A select field needs at least one option");
        f.setOptions(r.type() == FieldType.SELECT ? String.join("\n", opts) : null);
    }

    private CustomField requireField(Long wsId, Long id) {
        return fields.findById(id).filter(f -> f.getWorkspace().getId().equals(wsId)).orElseThrow(() -> ApiException.notFound("Custom field"));
    }

    /** Validates and stores custom field values for a saved task. Blank values clear the field. */
    public void applyValues(Task task, Map<Long, String> input) {
        if (input == null || input.isEmpty()) return;
        Long wsId = task.getProject().getWorkspace().getId();
        for (Map.Entry<Long, String> e : input.entrySet()) {
            CustomField f = requireField(wsId, e.getKey());
            String v = e.getValue() == null ? "" : e.getValue().trim();
            Optional<CustomFieldValue> existing = values.findByTaskIdAndFieldId(task.getId(), f.getId());
            if (v.isEmpty()) { existing.ifPresent(values::delete); continue; }
            validate(f, v);
            CustomFieldValue cv = existing.orElseGet(() -> {
                CustomFieldValue n = new CustomFieldValue();
                n.setTask(task); n.setField(f);
                return n;
            });
            cv.setValue(v);
            values.save(cv);
        }
    }

    private void validate(CustomField f, String v) {
        try {
            switch (f.getType()) {
                case NUMBER -> new BigDecimal(v);
                case DATE -> LocalDate.parse(v);
                case SELECT -> {
                    if (!Arrays.asList(f.getOptions().split("\n")).contains(v)) throw ApiException.badRequest("\"" + v + "\" is not an option for " + f.getName());
                }
                default -> { }
            }
        } catch (NumberFormatException | DateTimeParseException ex) {
            throw ApiException.badRequest("Invalid " + f.getType().name().toLowerCase() + " value for " + f.getName());
        }
    }
}
