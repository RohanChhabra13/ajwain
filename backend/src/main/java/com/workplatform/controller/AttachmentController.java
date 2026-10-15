package com.workplatform.controller;

import com.workplatform.dto.Dtos.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

import com.workplatform.entity.Attachment;
import com.workplatform.service.AttachmentService;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/attachments")
@RequiredArgsConstructor
public class AttachmentController {
    private final AttachmentService attachments;

    /** Always served as a download (never rendered inline) so an uploaded HTML/SVG file can't run in our origin. */
    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> download(@AuthenticationPrincipal Long uid, @PathVariable Long id) {
        Attachment a = attachments.requireForDownload(uid, id);
        Resource file = attachments.load(a);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(a.getFileName(), StandardCharsets.UTF_8).build().toString())
                .header("X-Content-Type-Options", "nosniff")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(file);
    }

    @DeleteMapping("/{id}") public void delete(@AuthenticationPrincipal Long uid, @PathVariable Long id) { attachments.delete(uid, id); }
}
