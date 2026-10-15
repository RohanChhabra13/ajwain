package com.workplatform.controller;

import com.workplatform.dto.Dtos.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

import com.workplatform.service.ProjectService;

@RestController
@RequestMapping("/api/epics")
@RequiredArgsConstructor
public class EpicController {
    private final ProjectService projects;

    @PutMapping("/{id}") public EpicResponse update(@AuthenticationPrincipal Long uid, @PathVariable Long id, @Valid @RequestBody EpicRequest r) { return projects.updateEpic(uid, id, r); }
    @DeleteMapping("/{id}") public void delete(@AuthenticationPrincipal Long uid, @PathVariable Long id) { projects.deleteEpic(uid, id); }
}
