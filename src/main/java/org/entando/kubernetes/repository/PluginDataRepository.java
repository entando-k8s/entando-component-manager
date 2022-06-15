package org.entando.kubernetes.repository;

import java.util.Optional;
import java.util.UUID;
import org.entando.kubernetes.model.job.PluginDataEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface PluginDataRepository extends JpaRepository<PluginDataEntity, UUID> {

    @Transactional
    long deleteByBundleCodeAndPluginCode(String bundleCode, String pluginCode);

    Optional<PluginDataEntity> findByBundleCodeAndPluginCode(String bundleCode, String pluginCode);
}
