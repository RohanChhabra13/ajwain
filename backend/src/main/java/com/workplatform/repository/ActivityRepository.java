package com.workplatform.repository;

import com.workplatform.entity.*;
import com.workplatform.entity.Enums.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.*;

public interface ActivityRepository extends JpaRepository<Activity, Long> {
    List<Activity> findByTaskIdOrderByCreatedAtDesc(Long taskId);
    List<Activity> findTop30ByProjectWorkspaceIdOrderByCreatedAtDesc(Long workspaceId);

    @Modifying(flushAutomatically = true)
    @Query("update Activity a set a.task = null where a.task.id = :taskId")
    void detachTask(@Param("taskId") Long taskId);
}
