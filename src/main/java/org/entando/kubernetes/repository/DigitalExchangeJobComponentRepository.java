package org.entando.kubernetes.repository;

import org.entando.kubernetes.model.digitalexchange.DigitalExchangeJob;
import org.entando.kubernetes.model.digitalexchange.DigitalExchangeJobComponent;
import org.entando.kubernetes.model.digitalexchange.JobStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Repository
public interface DigitalExchangeJobComponentRepository extends JpaRepository<DigitalExchangeJobComponent, UUID> {

    List<DigitalExchangeJobComponent> findAllByJob(DigitalExchangeJob job);

    @Modifying
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Query("UPDATE DigitalExchangeJobComponent comp SET comp.status = :status WHERE comp.id = :id")
    void updateJobStatus(@Param("id") UUID id, @Param("status") JobStatus status);

    @Modifying
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Query("UPDATE DigitalExchangeJobComponent comp SET comp.status = :status, comp.errorMessage = :errorMessage WHERE comp.id = :id")
    void updateJobStatus(@Param("id") UUID id, @Param("status") JobStatus status, @Param("errorMessage") String errorMessage);

}
