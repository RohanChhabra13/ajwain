package com.workplatform.dto;

import com.workplatform.entity.Enums.*;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public final class Dtos {
    private Dtos() {}

    // ----- auth -----
    public record RegisterRequest(@NotBlank String name, @Email @NotBlank String email,
                                  @NotBlank @Size(min = 8, max = 100) String password,
                                  String organizationName, String inviteToken) {}
    public record LoginRequest(@Email @NotBlank String email, @NotBlank String password) {}
    public record AuthResponse(String token, UserSummary user) {}
    public record UserSummary(Long id, String name, String email) {}
    public record UpdateProfileRequest(@NotBlank String name) {}
    public record ChangePasswordRequest(@NotBlank String currentPassword, @NotBlank @Size(min = 8, max = 100) String newPassword) {}

    // ----- organizations / workspaces / members -----
    public record CreateOrganizationRequest(@NotBlank String name) {}
    public record WorkspaceResponse(Long id, String name, Long organizationId, String organizationName, OrgRole myRole) {}
    public record CreateWorkspaceRequest(@NotNull Long organizationId, @NotBlank String name) {}
    public record RenameRequest(@NotBlank String name) {}
    public record MemberResponse(Long userId, String name, String email, OrgRole role) {}
    public record ChangeRoleRequest(@NotNull OrgRole role) {}
    public record InviteRequest(@Email @NotBlank String email, OrgRole role) {}
    public record InvitationResponse(Long id, String email, OrgRole role, String token, InviteStatus status, Instant expiresAt,
                                     Long workspaceId, String workspaceName, String organizationName) {}
    /** Either the person already had an account (member set) or an invitation link was created (invitation set). */
    public record InviteResult(boolean invited, MemberResponse member, InvitationResponse invitation) {}
    public record InvitationInfo(String email, String organizationName, String workspaceName, OrgRole role) {}

    // ----- workspace configuration -----
    public record StatusDto(Long id, String name, String color, int position, StatusCategory category) {}
    public record StatusRequest(@NotBlank String name, String color, Integer position, @NotNull StatusCategory category) {}
    public record PriorityDto(Long id, String name, int level, String color) {}
    public record PriorityRequest(@NotBlank String name, @NotNull Integer level, String color) {}
    public record CustomFieldDto(Long id, String name, FieldType type, List<String> options, int position) {}
    public record CustomFieldRequest(@NotBlank String name, @NotNull FieldType type, List<String> options) {}
    public record MetaResponse(List<StatusDto> statuses, List<PriorityDto> priorities, List<MemberResponse> members,
                               List<CustomFieldDto> customFields, List<String> tags) {}

    // ----- projects / epics -----
    public record ProjectRequest(@NotBlank String name, String description) {}
    public record ProjectResponse(Long id, String name, String description, ProjectStatus status, Long workspaceId) {}
    public record EpicRequest(@NotBlank String name, String description, EpicStatus status, LocalDate dueDate) {}
    public record EpicResponse(Long id, String name, String description, EpicStatus status, LocalDate dueDate,
                               Long projectId, long total, long done) {}

    // ----- tasks -----
    public record CreateTaskRequest(@NotBlank String title, String description, Long statusId, Long priorityId,
                                    LocalDate dueDate, LocalDate plannedDate, Long assigneeId, Long epicId,
                                    List<String> tags, Recurrence recurrence, Integer recurrenceInterval,
                                    Map<Long, String> customFields) {}
    /** Partial update: only non-null fields are applied. Use the clearX flags to unset a value. */
    public record UpdateTaskRequest(String title, String description, Long statusId, Long priorityId,
                                    LocalDate dueDate, Boolean clearDueDate, LocalDate plannedDate, Boolean clearPlannedDate,
                                    Long assigneeId, Boolean clearAssignee, Long epicId, Boolean clearEpic,
                                    List<String> tags, Recurrence recurrence, Integer recurrenceInterval,
                                    Map<Long, String> customFields) {}
    public record SubtaskSummary(Long id, String title, Long statusId, String statusName, StatusCategory category, UserSummary assignee) {}
    public record TaskResponse(Long id, String title, String description,
                               Long statusId, String statusName, String statusColor, StatusCategory category,
                               Long priorityId, String priorityName, int priorityLevel, String priorityColor,
                               LocalDate dueDate, LocalDate plannedDate, boolean overdue,
                               Long projectId, String projectName, Long epicId, String epicName, Long parentId,
                               UserSummary assignee, UserSummary createdBy, List<String> tags,
                               Recurrence recurrence, int recurrenceInterval, Map<Long, String> customFields,
                               int subtaskTotal, int subtaskDone, List<SubtaskSummary> subtasks,
                               Instant completedAt, Instant createdAt) {}
    /** Search filters. assignee: "me", "none" or a user id. */
    public record TaskFilter(String q, Long projectId, Long epicId, Long statusId, StatusCategory category, Long priorityId,
                             String assignee, String tag, LocalDate dueFrom, LocalDate dueTo, Boolean overdue,
                             Boolean includeSubtasks, Boolean includeArchived) {
        public static TaskFilter none() { return new TaskFilter(null, null, null, null, null, null, null, null, null, null, null, null, null); }
    }

    // ----- collaboration -----
    public record CommentRequest(@NotBlank @Size(max = 4000) String body) {}
    public record CommentResponse(Long id, String body, UserSummary author, Instant createdAt) {}
    public record ActivityResponse(Long id, String type, String message, UserSummary actor, Long taskId, Instant createdAt) {}
    public record AttachmentResponse(Long id, String fileName, String contentType, long size, UserSummary uploadedBy, Instant createdAt) {}
    public record NotificationResponse(Long id, String type, String message, Long taskId, Long projectId, boolean read, Instant createdAt) {}

    // ----- dashboard / my day -----
    public record StatusCount(Long statusId, String name, String color, StatusCategory category, long count) {}
    public record MemberProgress(Long userId, String name, long todo, long inProgress, long done) {}
    public record DashboardResponse(List<TaskResponse> dueToday, List<TaskResponse> overdue, List<StatusCount> statusCounts,
                                    List<MemberProgress> teamProgress, List<ActivityResponse> recentActivity) {}
    public record MyDayResponse(LocalDate date, List<TaskResponse> planned, List<TaskResponse> carryOver, List<TaskResponse> suggestions) {}

    // ----- reports -----
    public record DayCount(LocalDate date, long created, long completed) {}
    public record MemberReport(Long userId, String name, long assigned, long completed, long overdue) {}
    public record ProjectReport(Long projectId, String name, long total, long done) {}
    public record NameCount(String name, long count) {}
    public record ReportResponse(LocalDate from, LocalDate to, long created, long completed, long openOverdue, Double avgCycleDays,
                                 List<DayCount> daily, List<MemberReport> members, List<ProjectReport> projects, List<NameCount> openByPriority) {}
}
