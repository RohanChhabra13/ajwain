package com.workplatform.entity;

public final class Enums {
    private Enums() {}
    public enum OrgRole { OWNER, ADMIN, MEMBER }
    /** Semantic bucket for a (possibly custom) status. Drives "done", "overdue", dashboards and recurrence. */
    public enum StatusCategory { TODO, IN_PROGRESS, DONE }
    public enum ProjectStatus { ACTIVE, ARCHIVED }
    public enum EpicStatus { OPEN, DONE }
    public enum FieldType { TEXT, NUMBER, DATE, SELECT }
    public enum Recurrence { NONE, DAILY, WEEKLY, MONTHLY }
    public enum InviteStatus { PENDING, ACCEPTED, REVOKED }
}
