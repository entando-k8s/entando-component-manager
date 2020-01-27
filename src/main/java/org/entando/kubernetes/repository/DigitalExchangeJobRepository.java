package org.entando.kubernetes.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.entando.kubernetes.model.digitalexchange.DigitalExchangeJob;
import org.entando.kubernetes.model.digitalexchange.JobStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface DigitalExchangeJobRepository extends JpaRepository<DigitalExchangeJob, UUID> {

    @Query("SELECT job FROM DigitalExchangeJob job WHERE job.status <> :status AND job.componentId = :componentId")
    Optional<DigitalExchangeJob> findByComponentIdAndStatusNotEqual(@Param("componentId") String componentId,
                                                                    @Param("status") JobStatus status);


    Optional<DigitalExchangeJob> findFirstByComponentIdAndAndStatusNotOrderByStartedAtDesc(
            String componentId,
            JobStatus status);

    Optional<DigitalExchangeJob> findFirstByDigitalExchangeAndComponentIdOrderByStartedAtDesc(
            String digitalExchangeId,
            String componentId);

    List<DigitalExchangeJob> findAllByDigitalExchangeAndComponentIdOrderByStartedAtDesc(
            String digitalExchange,
            String componentId);

    List<DigitalExchangeJob> findAllByComponentIdOrderByStartedAtDesc(String componentId);

    @Modifying
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Query("UPDATE DigitalExchangeJob job SET job.status = :status WHERE job.id = :id")
    void updateJobStatus(@Param("id") UUID id, @Param("status") JobStatus status);


}
