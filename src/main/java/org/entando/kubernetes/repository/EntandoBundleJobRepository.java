package org.entando.kubernetes.repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.entando.kubernetes.model.job.EntandoBundleJobEntity;
import org.entando.kubernetes.model.job.JobStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface EntandoBundleJobRepository extends JpaRepository<EntandoBundleJobEntity, UUID> {

    List<EntandoBundleJobEntity> findAllByOrderByStartedAtDesc();

    List<EntandoBundleJobEntity> findAllByComponentIdOrderByStartedAtDesc(String componentId);

    Optional<EntandoBundleJobEntity> findFirstByComponentIdAndStatusInOrderByStartedAtDesc(String componentId,
            Set<JobStatus> status);

    Optional<EntandoBundleJobEntity> findFirstByComponentIdAndStatusOrderByStartedAtDesc(String componentId,
            JobStatus status);

    Optional<EntandoBundleJobEntity> findFirstByComponentIdOrderByStartedAtDesc(String componentId);

    @Modifying
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Query("UPDATE EntandoBundleJobEntity job SET job.status = :status WHERE job.id = :id")
    void updateJobStatus(@Param("id") UUID id, @Param("status") JobStatus status);

    Optional<List<EntandoBundleJobEntity>> findEntandoBundleJobEntityByIdIn(Set<UUID> idList);
}
