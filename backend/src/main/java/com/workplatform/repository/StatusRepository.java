package com.workplatform.repository;

import com.workplatform.entity.*;
import com.workplatform.entity.Enums.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.*;

public interface StatusRepository extends JpaRepository<Status, Long> {
    List<Status> findByWorkspaceIdOrderByPositionAsc(Long workspaceId);
    long countByWorkspaceIdAndCategory(Long workspaceId, StatusCategory category);
}
