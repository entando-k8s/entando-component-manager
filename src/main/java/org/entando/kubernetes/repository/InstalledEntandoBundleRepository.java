package org.entando.kubernetes.repository;

import java.util.Optional;
import org.entando.kubernetes.model.digitalexchange.EntandoBundle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface InstalledEntandoBundleRepository extends JpaRepository<EntandoBundle, String> {


    @Transactional
    void deleteById(String id);
}
