package org.entando.kubernetes.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.entando.kubernetes.model.job.PluginDataEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface PluginDataRepository extends JpaRepository<PluginDataEntity, UUID> {

    @Transactional
    long deleteByPluginCode(String pluginCode);

    Optional<PluginDataEntity> findByBundleIdAndPluginName(String bundleId, String pluginName);
    
    List<PluginDataEntity> findAllByBundleId(String bundleId);

}
