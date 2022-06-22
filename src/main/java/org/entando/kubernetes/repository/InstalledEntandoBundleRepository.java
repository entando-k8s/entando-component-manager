package org.entando.kubernetes.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.entando.kubernetes.model.job.EntandoBundleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface InstalledEntandoBundleRepository extends JpaRepository<EntandoBundleEntity, UUID> {

    @Transactional
    void deleteByBundleCode(String bundleCode);

    @Transactional
    List<EntandoBundleEntity> findAllByRepoUrlIn(List<String> repoUrls);

    @Transactional
    Optional<EntandoBundleEntity> findFirstByRepoUrl(String repoUrl);

    @Transactional
    List<EntandoBundleEntity> findAllByName(String name);

    @Transactional
    boolean existsByBundleCode(String bundleCode);

    @Transactional
    Optional<EntandoBundleEntity> findByBundleCode(String bundleCode);

}
