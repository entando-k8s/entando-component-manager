package org.entando.kubernetes.repository;

import org.entando.kubernetes.model.digitalexchange.DigitalExchangeJob;
import org.entando.kubernetes.model.digitalexchange.JobStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface DigitalExchangeJobRepository extends JpaRepository<DigitalExchangeJob, UUID> {

    @Query("SELECT job FROM DigitalExchangeJob job WHERE job.status <> :status AND job.componentId = :componentId")
    Optional<DigitalExchangeJob> findByComponentIdAndStatusNotEqual(@Param("componentId") String componentId,
                                                                    @Param("status") JobStatus status);

}
