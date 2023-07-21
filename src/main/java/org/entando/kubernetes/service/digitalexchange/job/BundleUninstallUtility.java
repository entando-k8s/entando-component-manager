package org.entando.kubernetes.service.digitalexchange.job;

import static org.entando.kubernetes.model.bundle.installable.Installable.MAX_COMMON_SIZE_OF_STRINGS;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.entando.kubernetes.client.model.EntandoCoreComponentDeleteResponse;
import org.entando.kubernetes.client.model.EntandoCoreComponentDeleteResponse.EntandoCoreComponentDelete;
import org.entando.kubernetes.client.model.EntandoCoreComponentDeleteResponse.EntandoCoreComponentDeleteStatus;
import org.entando.kubernetes.exception.EntandoComponentManagerException;
import org.entando.kubernetes.model.job.EntandoBundleComponentJobEntity;
import org.entando.kubernetes.model.job.JobResult;
import org.entando.kubernetes.repository.EntandoBundleComponentJobRepository;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class BundleUninstallUtility {

    private final @NonNull EntandoBundleComponentJobRepository compJobRepo;
    private final @NonNull ObjectMapper objectMapper;

    private static final String KEY_SEP = "_";



    public void markGlobalError(JobResult parentJobResult, EntandoCoreComponentDeleteResponse response) {
        try {
            String uninstallErrors = objectMapper.writeValueAsString(response.getComponents().stream()
                    .filter(c -> !EntandoCoreComponentDeleteStatus.SUCCESS.equals(c.getStatus())).collect(
                            Collectors.toList()));
            String valueToSave = StringUtils.truncate(uninstallErrors, MAX_COMMON_SIZE_OF_STRINGS);
            log.debug("save error inside parentJob:'{}'", valueToSave);
            parentJobResult.setUninstallErrors(valueToSave);
            parentJobResult.setUninstallException(new EntandoComponentManagerException("error inside delete from appEngine"));

        } catch (JsonProcessingException ex) {
            log.error("with response:'{}' we had a json error", response, ex);
        }
    }

    public void markSingleErrors(List<EntandoBundleComponentJobEntity> toDelete,
            EntandoCoreComponentDeleteResponse response) {
        // convenience map to speed up the search of an EntandoBundleComponentJobEntity related to a specific component
        // included in the app-engine response
        Map<String, EntandoBundleComponentJobEntity> toDeleteMap = toDelete.stream()
                .collect(Collectors.toMap(this::composeComponentJobEntityUniqueKey, Function.identity()));

        response.getComponents()
                .stream()
                .filter(c -> EntandoCoreComponentDeleteStatus.FAILURE.equals(c.getStatus()))
                .map(entandoCoreComponentDelete ->
                        // find the EntandoBundleComponentJobEntity, related to the ith component of the response,
                        // to set on it the appropriate error message
                        Optional.ofNullable(
                                        toDeleteMap.get(composeComponentDeleteUniqueKey(entandoCoreComponentDelete)))
                                .orElseThrow(() -> new IllegalStateException(
                                        String.format("Missing job for component '%s' of type '%s'",
                                                entandoCoreComponentDelete.getCode(),
                                                entandoCoreComponentDelete.getType())))
                ).forEach(cje -> {
                    // set the error message and code and save to DB
                    cje.setUninstallErrorMessage("Error in deleting component from app-engine");
                    cje.setUninstallErrorCode(100);
                    compJobRepo.save(cje);
                });
    }

    private String composeComponentDeleteUniqueKey(EntandoCoreComponentDelete entandoCoreComponentDelete) {
        if (entandoCoreComponentDelete == null
                || StringUtils.isEmpty(entandoCoreComponentDelete.getCode())
                || Objects.isNull(entandoCoreComponentDelete.getType())) {
            throw new IllegalArgumentException("Error in composing key from Entando Core Component Delete: "
                    + "element, code or type fields have null or empty values");
        }
        return entandoCoreComponentDelete.getCode() + KEY_SEP + entandoCoreComponentDelete.getType();
    }

    private String composeComponentJobEntityUniqueKey(EntandoBundleComponentJobEntity componentJobEntity) {
        if (componentJobEntity == null
                || StringUtils.isEmpty(componentJobEntity.getComponentId())
                || Objects.isNull(componentJobEntity.getComponentType())) {
            throw new IllegalArgumentException("Error in composing key from Entando Bundle Component Job Entity: "
                    + "element, code or type fields have null or empty values");
        }
        return componentJobEntity.getComponentId() + KEY_SEP + componentJobEntity.getComponentType();
    }

}
