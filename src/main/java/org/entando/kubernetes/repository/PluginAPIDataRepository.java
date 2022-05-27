package org.entando.kubernetes.repository;

import java.util.Optional;
import java.util.UUID;
import org.entando.kubernetes.model.job.PluginAPIDataEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface PluginAPIDataRepository extends JpaRepository<PluginAPIDataEntity, UUID> {

    @Transactional
    void deleteByBundleIdAndServiceId(String bundleId, String serviceId);

    Optional<PluginAPIDataEntity> findByBundleIdAndServiceId(String bundleId, String serviceId);
}
