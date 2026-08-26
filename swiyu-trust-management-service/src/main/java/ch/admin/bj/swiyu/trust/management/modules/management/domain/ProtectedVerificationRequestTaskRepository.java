package ch.admin.bj.swiyu.trust.management.modules.management.domain;

import java.util.*;
import org.springframework.data.jpa.repository.*;

public interface ProtectedVerificationRequestTaskRepository
    extends JpaRepository<ProtectedVerificationRequestTask, UUID>
{
    void deleteAllByPartnerId(UUID partnerId);
}
