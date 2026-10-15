package com.workplatform.repository;

import com.workplatform.entity.*;
import com.workplatform.entity.Enums.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.*;

public interface TaskRepository extends JpaRepository<Task, Long>, JpaSpecificationExecutor<Task> {
    List<Task> findByParentIdOrderByCreatedAtAsc(Long parentId);
    List<Task> findByParentIdIn(Collection<Long> parentIds);
    List<Task> findByProjectWorkspaceId(Long workspaceId);
    long countByStatusId(Long statusId);
    long countByPriorityId(Long priorityId);

    @Query("select t.parent.id, count(t), sum(case when t.status.category = :done then 1 else 0 end) from Task t where t.parent.id in :ids group by t.parent.id")
    List<Object[]> subtaskCounts(@Param("ids") Collection<Long> ids, @Param("done") StatusCategory done);

    @Query("select e.id, count(t), sum(case when t.status.category = :done then 1 else 0 end) from Task t join t.epic e where e.project.id = :pid group by e.id")
    List<Object[]> epicCounts(@Param("pid") Long projectId, @Param("done") StatusCategory done);

    // ----- My work / dashboard -----
    @Query("select t from Task t where t.project.workspace.id = :ws and t.assignee.id = :uid and t.project.status = :active order by t.dueDate asc nulls last, t.createdAt desc")
    List<Task> findMine(@Param("ws") Long ws, @Param("uid") Long uid, @Param("active") ProjectStatus active);

    @Query("select t from Task t where t.project.workspace.id = :ws and t.dueDate = :day and t.status.category <> :done and t.project.status = :active order by t.createdAt")
    List<Task> findDueOn(@Param("ws") Long ws, @Param("day") LocalDate day, @Param("done") StatusCategory done, @Param("active") ProjectStatus active);

    @Query("select t from Task t where t.project.workspace.id = :ws and t.dueDate < :day and t.status.category <> :done and t.project.status = :active order by t.dueDate asc")
    List<Task> findOverdue(@Param("ws") Long ws, @Param("day") LocalDate day, @Param("done") StatusCategory done, @Param("active") ProjectStatus active);

    @Query("select t.status.id, count(t) from Task t where t.project.workspace.id = :ws and t.project.status = :active group by t.status.id")
    List<Object[]> countByStatus(@Param("ws") Long ws, @Param("active") ProjectStatus active);

    @Query("select t.assignee.id, t.assignee.name, t.status.category, count(t) from Task t where t.project.workspace.id = :ws and t.assignee is not null and t.project.status = :active group by t.assignee.id, t.assignee.name, t.status.category")
    List<Object[]> countByAssignee(@Param("ws") Long ws, @Param("active") ProjectStatus active);

    // ----- My Day -----
    @Query("select t from Task t where t.project.workspace.id = :ws and t.assignee.id = :uid and t.plannedDate = :day order by t.createdAt")
    List<Task> findPlannedOn(@Param("ws") Long ws, @Param("uid") Long uid, @Param("day") LocalDate day);

    @Query("select t from Task t where t.project.workspace.id = :ws and t.assignee.id = :uid and t.plannedDate < :day and t.status.category <> :done and t.project.status = :active order by t.plannedDate")
    List<Task> findCarryOver(@Param("ws") Long ws, @Param("uid") Long uid, @Param("day") LocalDate day, @Param("done") StatusCategory done, @Param("active") ProjectStatus active);

    @Query("select t from Task t where t.project.workspace.id = :ws and t.assignee.id = :uid and t.dueDate <= :day and t.status.category <> :done and t.project.status = :active and (t.plannedDate is null or t.plannedDate <> :day) order by t.dueDate")
    List<Task> findSuggestions(@Param("ws") Long ws, @Param("uid") Long uid, @Param("day") LocalDate day, @Param("done") StatusCategory done, @Param("active") ProjectStatus active);

    // ----- Reminders -----
    @Query("select t from Task t where t.dueDate <= :day and t.assignee is not null and t.status.category <> :done and t.project.status = :active")
    List<Task> findDueForReminders(@Param("day") LocalDate day, @Param("done") StatusCategory done, @Param("active") ProjectStatus active);

    // ----- Bulk maintenance -----
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update Task t set t.assignee = null where t.assignee.id = :uid and t.project.id in (select p.id from Project p where p.workspace.id = :ws)")
    int unassignInWorkspace(@Param("uid") Long uid, @Param("ws") Long ws);

    @Modifying(flushAutomatically = true)
    @Query("update Task t set t.epic = null where t.epic.id = :eid")
    void clearEpic(@Param("eid") Long epicId);
}
