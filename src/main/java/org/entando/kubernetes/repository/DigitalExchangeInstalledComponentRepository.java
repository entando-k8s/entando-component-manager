package org.entando.kubernetes.repository;

import java.util.UUID;
import org.entando.kubernetes.model.digitalexchange.DigitalExchangeComponent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DigitalExchangeInstalledComponentRepository extends JpaRepository<DigitalExchangeComponent, UUID> {

}
