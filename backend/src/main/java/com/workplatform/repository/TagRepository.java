package com.workplatform.repository;

import com.workplatform.entity.*;
import com.workplatform.entity.Enums.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.*;

public interface TagRepository extends JpaRepository<Tag, Long> {
    Optional<Tag> findByWorkspaceIdAndNameIgnoreCase(Long workspaceId, String name);
    List<Tag> findByWorkspaceIdOrderByNameAsc(Long workspaceId);
}
