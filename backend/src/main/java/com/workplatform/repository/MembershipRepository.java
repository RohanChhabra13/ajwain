package com.workplatform.repository;

import com.workplatform.entity.*;
import com.workplatform.entity.Enums.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.*;

public interface MembershipRepository extends JpaRepository<Membership, Long> {
    List<Membership> findByUserId(Long userId);
    List<Membership> findByOrganizationId(Long organizationId);
    Optional<Membership> findByUserIdAndOrganizationId(Long userId, Long organizationId);
}
