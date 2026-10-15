package com.workplatform.service;

import com.workplatform.dto.Dtos.AttachmentResponse;
import com.workplatform.entity.*;
import com.workplatform.exception.ApiException;
import com.workplatform.repository.*;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class AttachmentService {
    private final AccessService access;
    private final AttachmentRepository attachments;
    private final UserRepository users;
    private final ActivityLogger logger;
    private final Path root;

    public AttachmentService(AccessService access, AttachmentRepository attachments, UserRepository users, ActivityLogger logger,
                             @Value("${app.storage.dir:./data/uploads}") String dir) {
        this.access = access; this.attachments = attachments; this.users = users; this.logger = logger;
        this.root = Paths.get(dir).toAbsolutePath().normalize();
    }

    @PostConstruct
    void init() throws IOException { Files.createDirectories(root); }

    @Transactional(readOnly = true)
    public List<AttachmentResponse> list(Long userId, Long taskId) {
        access.requireTask(userId, taskId);
        return attachments.findByTaskIdOrderByCreatedAtAsc(taskId).stream().map(Mapper::attachment).toList();
    }

    public AttachmentResponse upload(Long userId, Long taskId, MultipartFile file) {
        Task t = access.requireTask(userId, taskId);
        if (file == null || file.isEmpty()) throw ApiException.badRequest("Choose a file to upload");
        String key = UUID.randomUUID().toString();
        try {
            Files.copy(file.getInputStream(), root.resolve(key), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new ApiException(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR, "Could not store the file");
        }
        Attachment a = new Attachment();
        a.setFileName(cleanName(file.getOriginalFilename()));
        a.setContentType(file.getContentType());
        a.setSize(file.getSize());
        a.setStorageKey(key);
        a.setTask(t);
        a.setUploadedBy(users.getReferenceById(userId));
        attachments.save(a);
        logger.log(t.getProject(), t, userId, "ATTACHMENT_ADDED", "attached \"" + a.getFileName() + "\" to \"" + t.getTitle() + "\"");
        return Mapper.attachment(a);
    }

    @Transactional(readOnly = true)
    public Attachment requireForDownload(Long userId, Long attachmentId) {
        Attachment a = attachments.findById(attachmentId).orElseThrow(() -> ApiException.notFound("Attachment"));
        access.requireTask(userId, a.getTask().getId());
        return a;
    }

    public Resource load(Attachment a) {
        Path p = root.resolve(a.getStorageKey()).normalize();
        if (!p.startsWith(root) || !Files.exists(p)) throw ApiException.notFound("File");
        return new FileSystemResource(p);
    }

    public void delete(Long userId, Long attachmentId) {
        Attachment a = attachments.findById(attachmentId).orElseThrow(() -> ApiException.notFound("Attachment"));
        Task t = access.requireTask(userId, a.getTask().getId());
        boolean owner = a.getUploadedBy().getId().equals(userId);
        if (!owner) access.requireWorkspaceManager(userId, t.getProject().getWorkspace().getId());
        removeFile(a);
        attachments.delete(a);
    }

    /** Called when a task is deleted. */
    public void deleteAllFor(Long taskId) {
        for (Attachment a : attachments.findByTaskIdOrderByCreatedAtAsc(taskId)) { removeFile(a); attachments.delete(a); }
    }

    private void removeFile(Attachment a) {
        try { Files.deleteIfExists(root.resolve(a.getStorageKey()).normalize()); } catch (IOException ignored) { }
    }

    static String cleanName(String original) {
        String n = original == null ? "file" : original;
        n = n.replace('\\', '/');
        n = n.substring(n.lastIndexOf('/') + 1).replaceAll("[\\p{Cntrl}\"]", "_").trim();
        if (n.isEmpty()) n = "file";
        return n.length() > 200 ? n.substring(n.length() - 200) : n;
    }
}
