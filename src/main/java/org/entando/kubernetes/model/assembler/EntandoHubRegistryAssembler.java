package org.entando.kubernetes.model.assembler;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;
import org.entando.kubernetes.exception.EntandoComponentManagerException;
import org.entando.kubernetes.model.entandohub.EntandoHubRegistry;
import org.entando.kubernetes.model.entandohub.EntandoHubRegistryEntity;
import org.springframework.util.ObjectUtils;

@UtilityClass
public class EntandoHubRegistryAssembler {

    public static EntandoHubRegistry toEntandoHubRegistry(EntandoHubRegistryEntity entity) {

        if (entity == null) {
            throw new EntandoComponentManagerException("The received EntandoHubRegistryEntity to convert is null");
        }

        EntandoHubRegistry entandoHubRegistry = new EntandoHubRegistry()
                .setId(entity.getId().toString())
                .setName(entity.getName())
                .setUrl(entity.getUrl().toString());
        entandoHubRegistry.setApiKeyPresent(StringUtils.isNotEmpty(entity.getApiKey()));
        return entandoHubRegistry;
    }

    public static List<EntandoHubRegistry> toListOfEntandoHubRegistry(List<EntandoHubRegistryEntity> entityList) {

        return Optional.ofNullable(entityList)
                .orElseGet(ArrayList::new)
                .stream().map(EntandoHubRegistryAssembler::toEntandoHubRegistry)
                .collect(Collectors.toList());
    }

    public static EntandoHubRegistryEntity toEntandoHubRegistryEntity(EntandoHubRegistry dto) {

        if (dto == null) {
            throw new EntandoComponentManagerException("The received EntandoHubRegistry to convert is null");
        }

        EntandoHubRegistryEntity entandoHubRegistryEntity = new EntandoHubRegistryEntity()
                .setName(dto.getName())
                .setUrl(dto.getUrlAsURL());

        if (! ObjectUtils.isEmpty(dto.getId())) {
            entandoHubRegistryEntity.setId(UUID.fromString(dto.getId()));
        }

        if (!ObjectUtils.isEmpty(dto.getApiKey())) {
            entandoHubRegistryEntity.setApiKey(dto.getApiKey());
        }

        return entandoHubRegistryEntity;
    }
}
