package com.workplatform.repository;

import com.workplatform.entity.*;
import com.workplatform.entity.Enums.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.*;

public interface CustomFieldValueRepository extends JpaRepository<CustomFieldValue, Long> {
    List<CustomFieldValue> findByTaskId(Long taskId);
    List<CustomFieldValue> findByTaskIdIn(Collection<Long> taskIds);
    Optional<CustomFieldValue> findByTaskIdAndFieldId(Long taskId, Long fieldId);
    void deleteByTaskId(Long taskId);
    void deleteByFieldId(Long fieldId);
}
