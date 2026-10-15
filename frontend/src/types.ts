// Shapes returned by the Spring Boot API (mirrors backend/src/main/java/com/workplatform/dto/Dtos.java).

export type OrgRole = 'OWNER' | 'ADMIN' | 'MEMBER'
export type StatusCategory = 'TODO' | 'IN_PROGRESS' | 'DONE'
export type Recurrence = 'NONE' | 'DAILY' | 'WEEKLY' | 'MONTHLY'
export type FieldType = 'TEXT' | 'NUMBER' | 'DATE' | 'SELECT'

export interface User { id: number; name: string; email: string }
export interface AuthResponse { token: string; user: User }

export interface Workspace { id: number; name: string; organizationId: number; organizationName: string; myRole: OrgRole }
export interface Member { userId: number; name: string; email: string; role: OrgRole }
export interface Status { id: number; name: string; color: string | null; position: number; category: StatusCategory }
export interface Priority { id: number; name: string; level: number; color: string | null }
export interface CustomField { id: number; name: string; type: FieldType; options: string[]; position: number }
export interface Meta { statuses: Status[]; priorities: Priority[]; members: Member[]; customFields: CustomField[]; tags: string[] }

export interface Project { id: number; name: string; description: string | null; status: 'ACTIVE' | 'ARCHIVED'; workspaceId: number }
export interface Epic { id: number; name: string; description: string | null; status: 'OPEN' | 'DONE'; dueDate: string | null; projectId: number; total: number; done: number }

export interface Subtask { id: number; title: string; statusId: number; statusName: string; category: StatusCategory; assignee: User | null }
export interface Task {
  id: number; title: string; description: string | null
  statusId: number; statusName: string; statusColor: string | null; category: StatusCategory
  priorityId: number; priorityName: string; priorityLevel: number; priorityColor: string | null
  dueDate: string | null; plannedDate: string | null; overdue: boolean
  projectId: number; projectName: string; epicId: number | null; epicName: string | null; parentId: number | null
  assignee: User | null; createdBy: User | null; tags: string[]
  recurrence: Recurrence; recurrenceInterval: number; customFields: Record<string, string>
  subtaskTotal: number; subtaskDone: number; subtasks: Subtask[]
  completedAt: string | null; createdAt: string
}

export interface Comment { id: number; body: string; author: User; createdAt: string }
export interface Activity { id: number; type: string; message: string; actor: User; taskId: number | null; createdAt: string }
export interface Attachment { id: number; fileName: string; contentType: string | null; size: number; uploadedBy: User; createdAt: string }
export interface Notification { id: number; type: string; message: string; taskId: number | null; projectId: number | null; read: boolean; createdAt: string }

export interface Invitation { id: number; email: string; role: OrgRole; token: string; status: string; expiresAt: string; workspaceId: number; workspaceName: string; organizationName: string }
export interface InvitationInfo { email: string; organizationName: string; workspaceName: string; role: OrgRole }
export interface InviteResult { invited: boolean; member: Member | null; invitation: Invitation | null }

export interface StatusCount { statusId: number; name: string; color: string | null; category: StatusCategory; count: number }
export interface MemberProgress { userId: number; name: string; todo: number; inProgress: number; done: number }
export interface Dashboard { dueToday: Task[]; overdue: Task[]; statusCounts: StatusCount[]; teamProgress: MemberProgress[]; recentActivity: Activity[] }
export interface MyDay { date: string; planned: Task[]; carryOver: Task[]; suggestions: Task[] }

export interface Report {
  from: string; to: string; created: number; completed: number; openOverdue: number; avgCycleDays: number | null
  daily: { date: string; created: number; completed: number }[]
  members: { userId: number; name: string; assigned: number; completed: number; overdue: number }[]
  projects: { projectId: number; name: string; total: number; done: number }[]
  openByPriority: { name: string; count: number }[]
}

/** Partial update accepted by PATCH /tasks/{id}. */
export interface TaskPatch {
  title?: string; description?: string; statusId?: number; priorityId?: number
  dueDate?: string; clearDueDate?: boolean; plannedDate?: string; clearPlannedDate?: boolean
  assigneeId?: number; clearAssignee?: boolean; epicId?: number; clearEpic?: boolean
  tags?: string[]; recurrence?: Recurrence; recurrenceInterval?: number; customFields?: Record<string, string>
}
