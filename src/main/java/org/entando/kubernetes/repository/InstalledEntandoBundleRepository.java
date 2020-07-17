package org.entando.kubernetes.repository;

import java.util.Optional;
import java.util.UUID;
import org.entando.kubernetes.model.digitalexchange.EntandoBundleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface InstalledEntandoBundleRepository extends JpaRepository<EntandoBundleEntity, UUID> {

    Optional<EntandoBundleEntity> findByEcrId(String ecrId);

    @Transactional
    void deleteById(String UUID);

}
