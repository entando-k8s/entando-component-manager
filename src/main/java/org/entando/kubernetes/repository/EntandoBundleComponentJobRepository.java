package org.entando.kubernetes.repository;

import java.util.List;
import java.util.UUID;
import org.entando.kubernetes.model.digitalexchange.EntandoBundleJob;
import org.entando.kubernetes.model.digitalexchange.EntandoBundleComponentJob;
import org.entando.kubernetes.model.digitalexchange.JobStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface EntandoBundleComponentJobRepository extends JpaRepository<EntandoBundleComponentJob, UUID> {

    List<EntandoBundleComponentJob> findAllByJob(EntandoBundleJob job);

    @Modifying
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Query("UPDATE EntandoBundleComponentJob comp SET comp.status = :status WHERE comp.id = :id")
    void updateJobStatus(@Param("id") UUID id, @Param("status") JobStatus status);

    @Modifying
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Query("UPDATE EntandoBundleComponentJob comp SET comp.status = :status, comp.errorMessage = :errorMessage WHERE comp.id = :id")
    void updateJobStatus(@Param("id") UUID id, @Param("status") JobStatus status, @Param("errorMessage") String errorMessage);

}
