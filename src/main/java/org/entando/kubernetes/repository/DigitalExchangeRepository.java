package org.entando.kubernetes.repository;

import org.entando.kubernetes.model.digitalexchange.DigitalExchangeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface DigitalExchangeRepository extends JpaRepository<DigitalExchangeEntity, UUID> {
}
