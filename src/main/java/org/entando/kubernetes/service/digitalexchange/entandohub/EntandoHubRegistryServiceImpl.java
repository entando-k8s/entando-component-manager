/*
 * Copyright 2018-Present Entando Inc. (http://www.entando.com) All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package org.entando.kubernetes.service.digitalexchange.entandohub;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.entando.kubernetes.exception.EntandoClientDataException;
import org.entando.kubernetes.exception.web.NotFoundException;
import org.entando.kubernetes.model.assembler.EntandoHubRegistryAssembler;
import org.entando.kubernetes.model.entandohub.EntandoHubRegistry;
import org.entando.kubernetes.model.entandohub.EntandoHubRegistryEntity;
import org.entando.kubernetes.repository.EntandoHubRegistryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class EntandoHubRegistryServiceImpl implements EntandoHubRegistryService {

    private static final String REGISTRY_NAME_ALREADY_PRESENT = "An Entando Hub registry with this name is already present";
    private static final String REGISTRY_URL_ALREADY_PRESENT = "An Entando Hub registry with this url is already present";

    private final EntandoHubRegistryRepository repository;

    @Autowired
    public EntandoHubRegistryServiceImpl(EntandoHubRegistryRepository repository) {
        this.repository = repository;
    }

//    @Override
    public EntandoHubRegistry getRegistry(String id) {
        return repository.findById(UUID.fromString(id))
                .map(EntandoHubRegistryAssembler::toEntandoHubRegistry)
                .orElseThrow(() -> new NotFoundException("No registry found with the given ID"));
    }

    @Override
    public List<EntandoHubRegistry> listRegistries() {
        return EntandoHubRegistryAssembler.toListOfEntandoHubRegistry(
                repository.findAllByOrderByNameAsc());
    }

    @Override
    public EntandoHubRegistry createRegistry(EntandoHubRegistry entandoHubRegistry) {

        if (repository.findByName(entandoHubRegistry.getName()).isPresent()) {
            throw new EntandoClientDataException(REGISTRY_NAME_ALREADY_PRESENT);
        }
        if (repository.findByUrl(entandoHubRegistry.getUrlAsURL()).isPresent()) {
            throw new EntandoClientDataException(REGISTRY_URL_ALREADY_PRESENT);
        }

        final EntandoHubRegistryEntity entity = EntandoHubRegistryAssembler.toEntandoHubRegistryEntity(
                entandoHubRegistry);
        return EntandoHubRegistryAssembler.toEntandoHubRegistry(repository.save(entity));
    }

    @Override
    public EntandoHubRegistry updateRegistry(EntandoHubRegistry entandoHubRegistry) {

        UUID uuid = UUID.fromString(entandoHubRegistry.getId());

        if (repository.findByNameAndIdNot(entandoHubRegistry.getName(), uuid).isPresent()) {
            throw new EntandoClientDataException(REGISTRY_NAME_ALREADY_PRESENT);
        }
        if (repository.findByUrlAndIdNot(entandoHubRegistry.getUrlAsURL(), uuid).isPresent()) {
            throw new EntandoClientDataException(REGISTRY_URL_ALREADY_PRESENT);
        }

        return repository.findById(UUID.fromString(entandoHubRegistry.getId()))
                .map(entity -> {
                    entity.setName(entandoHubRegistry.getName())
                            .setUrl(entandoHubRegistry.getUrlAsURL());
                    return EntandoHubRegistryAssembler.toEntandoHubRegistry(repository.save(entity));
                })
                .orElseThrow(() -> new NotFoundException("No registry found for the received ID"));
    }

    @Override
    public String deleteRegistry(String id) {
        final UUID uuid = UUID.fromString(id);
        final Optional<EntandoHubRegistryEntity> registryToDelete = repository.findById(uuid);
        if (registryToDelete.isPresent()) {
            repository.delete(registryToDelete.get());
            return registryToDelete.get().getName();
        } else {
            return "";
        }
    }
}
