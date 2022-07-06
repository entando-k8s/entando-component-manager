package org.entando.kubernetes.repository;

import java.util.Optional;
import java.util.UUID;
import org.entando.kubernetes.model.bundle.ComponentType;
import org.entando.kubernetes.model.job.ComponentDataEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ComponentDataRepository extends JpaRepository<ComponentDataEntity, UUID> {

    Optional<ComponentDataEntity> findByComponentTypeAndComponentCode(ComponentType componentType,
            String componentCode);

}
