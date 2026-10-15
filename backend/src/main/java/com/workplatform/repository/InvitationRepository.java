package com.workplatform.repository;

import com.workplatform.entity.*;
import com.workplatform.entity.Enums.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.*;

public interface InvitationRepository extends JpaRepository<Invitation, Long> {
    Optional<Invitation> findByToken(String token);
    List<Invitation> findByWorkspaceIdAndStatusOrderByCreatedAtDesc(Long workspaceId, InviteStatus status);
    Optional<Invitation> findFirstByWorkspaceIdAndEmailIgnoreCaseAndStatus(Long workspaceId, String email, InviteStatus status);
}
