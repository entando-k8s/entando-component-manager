package org.entando.kubernetes.repository;

import java.util.List;
import org.entando.kubernetes.model.job.EntandoBundleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface InstalledEntandoBundleRepository extends JpaRepository<EntandoBundleEntity, String> {

    @Transactional
    void deleteById(String id);

    List<EntandoBundleEntity> findAllByIdIn(List<String> ids);
}
