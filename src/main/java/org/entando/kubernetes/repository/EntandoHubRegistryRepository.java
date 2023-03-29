package org.entando.kubernetes.repository;

import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.entando.kubernetes.model.entandohub.EntandoHubRegistryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface EntandoHubRegistryRepository extends JpaRepository<EntandoHubRegistryEntity, UUID> {

//    Optional<EntandoHubRegistryEntity> findById(@Param("id") String id);

    List<EntandoHubRegistryEntity> findAllByOrderByNameAsc();

    Optional<EntandoHubRegistryEntity> findByName(@Param("name") String name);

    Optional<EntandoHubRegistryEntity> findByNameAndIdNot(@Param("name") String name, @Param("id") UUID uuid);

    Optional<EntandoHubRegistryEntity> findByUrl(@Param("url") URL url);

    Optional<EntandoHubRegistryEntity> findByUrlAndIdNot(@Param("url") URL url, @Param("id") UUID uuid);
}
