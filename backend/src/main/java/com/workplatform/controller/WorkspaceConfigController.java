package com.workplatform.controller;

import com.workplatform.dto.Dtos.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

import com.workplatform.service.WorkspaceConfigService;

@RestController
@RequestMapping("/api/workspaces/{ws}")
@RequiredArgsConstructor
public class WorkspaceConfigController {
    private final WorkspaceConfigService cfg;

    @GetMapping("/statuses") public List<StatusDto> statuses(@AuthenticationPrincipal Long uid, @PathVariable Long ws) { return cfg.listStatuses(uid, ws); }
    @PostMapping("/statuses") public StatusDto createStatus(@AuthenticationPrincipal Long uid, @PathVariable Long ws, @Valid @RequestBody StatusRequest r) { return cfg.createStatus(uid, ws, r); }
    @PutMapping("/statuses/{id}") public StatusDto updateStatus(@AuthenticationPrincipal Long uid, @PathVariable Long ws, @PathVariable Long id, @Valid @RequestBody StatusRequest r) { return cfg.updateStatus(uid, ws, id, r); }
    @DeleteMapping("/statuses/{id}") public void deleteStatus(@AuthenticationPrincipal Long uid, @PathVariable Long ws, @PathVariable Long id) { cfg.deleteStatus(uid, ws, id); }

    @GetMapping("/priorities") public List<PriorityDto> priorities(@AuthenticationPrincipal Long uid, @PathVariable Long ws) { return cfg.listPriorities(uid, ws); }
    @PostMapping("/priorities") public PriorityDto createPriority(@AuthenticationPrincipal Long uid, @PathVariable Long ws, @Valid @RequestBody PriorityRequest r) { return cfg.createPriority(uid, ws, r); }
    @PutMapping("/priorities/{id}") public PriorityDto updatePriority(@AuthenticationPrincipal Long uid, @PathVariable Long ws, @PathVariable Long id, @Valid @RequestBody PriorityRequest r) { return cfg.updatePriority(uid, ws, id, r); }
    @DeleteMapping("/priorities/{id}") public void deletePriority(@AuthenticationPrincipal Long uid, @PathVariable Long ws, @PathVariable Long id) { cfg.deletePriority(uid, ws, id); }

    @GetMapping("/custom-fields") public List<CustomFieldDto> fields(@AuthenticationPrincipal Long uid, @PathVariable Long ws) { return cfg.listFields(uid, ws); }
    @PostMapping("/custom-fields") public CustomFieldDto createField(@AuthenticationPrincipal Long uid, @PathVariable Long ws, @Valid @RequestBody CustomFieldRequest r) { return cfg.createField(uid, ws, r); }
    @PutMapping("/custom-fields/{id}") public CustomFieldDto updateField(@AuthenticationPrincipal Long uid, @PathVariable Long ws, @PathVariable Long id, @Valid @RequestBody CustomFieldRequest r) { return cfg.updateField(uid, ws, id, r); }
    @DeleteMapping("/custom-fields/{id}") public void deleteField(@AuthenticationPrincipal Long uid, @PathVariable Long ws, @PathVariable Long id) { cfg.deleteField(uid, ws, id); }
}
