package org.entando.kubernetes.repository;

import java.util.List;
import java.util.Optional;
import org.entando.kubernetes.model.job.EntandoBundleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface InstalledEntandoBundleRepository extends JpaRepository<EntandoBundleEntity, String> {

    @Transactional
    void deleteById(String id);

    @Transactional
    List<EntandoBundleEntity> findAllByRepoUrlIn(List<String> repoUrls);

    @Transactional
    Optional<EntandoBundleEntity> findFirstByRepoUrl(String repoUrl);

    @Transactional
    List<EntandoBundleEntity> findAllByName(String name);
}
