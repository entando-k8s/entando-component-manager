package org.entando.kubernetes.service.digitalexchange.component;

import static org.entando.kubernetes.model.bundle.ComponentType.CATEGORY;
import static org.entando.kubernetes.model.bundle.ComponentType.CONTENT_TEMPLATE;
import static org.entando.kubernetes.model.bundle.ComponentType.CONTENT_TYPE;
import static org.entando.kubernetes.model.bundle.ComponentType.FRAGMENT;
import static org.entando.kubernetes.model.bundle.ComponentType.GROUP;
import static org.entando.kubernetes.model.bundle.ComponentType.PAGE;
import static org.entando.kubernetes.model.bundle.ComponentType.PAGE_TEMPLATE;
import static org.entando.kubernetes.model.bundle.ComponentType.WIDGET;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.entando.kubernetes.client.core.EntandoCoreClient;
import org.entando.kubernetes.model.bundle.ComponentType;
import org.entando.kubernetes.model.bundle.usage.ComponentUsage;
import org.entando.kubernetes.model.bundle.usage.ComponentUsage.ComponentReference;
import org.entando.kubernetes.model.bundle.usage.ComponentUsage.ComponentReferenceType;
import org.entando.kubernetes.model.entandocore.EntandoCoreComponentUsage;
import org.entando.kubernetes.model.entandocore.EntandoCoreComponentUsage.IrrelevantComponentUsage;
import org.entando.kubernetes.model.entandocore.EntandoCoreComponentUsageRequest;
import org.entando.kubernetes.model.job.EntandoBundleComponentJobEntity;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class EntandoBundleComponentUsageService {

    private final EntandoCoreClient client;
    private static final EnumSet<ComponentType> RELEVANT_USAGE_COMPONENT_TYPES = EnumSet.of(WIDGET, GROUP, FRAGMENT,
            CATEGORY, PAGE, PAGE_TEMPLATE, CONTENT_TYPE, CONTENT_TEMPLATE);
    private static final String KEY_SEP = "_";

    public EntandoBundleComponentUsageService(EntandoCoreClient client) {
        this.client = client;
    }

    public EntandoCoreComponentUsage getUsage(ComponentType componentType, String componentCode) {
        switch (componentType) {
            case WIDGET:
                return this.client.getWidgetUsage(componentCode);
            case GROUP:
                return this.client.getGroupUsage(componentCode);
            case FRAGMENT:
                return this.client.getFragmentUsage(componentCode);
            case CATEGORY:
                return this.client.getCategoryUsage(componentCode);
            case PAGE:
                return this.client.getPageUsage(componentCode);
            case PAGE_TEMPLATE:
                return this.client.getPageModelUsage(componentCode);
            case CONTENT_TYPE:
                return this.client.getContentTypeUsage(componentCode);
            case CONTENT_TEMPLATE:
                return this.client.getContentModelUsage(componentCode);
            default:
                return new IrrelevantComponentUsage(componentCode);
        }
    }

    public List<ComponentUsage> getComponentsUsageDetails(
            List<EntandoBundleComponentJobEntity> bundleInstalledComponents) {

        List<EntandoCoreComponentUsageRequest> usageListRequest = bundleInstalledComponents.stream()
                .filter(u -> RELEVANT_USAGE_COMPONENT_TYPES.contains(u.getComponentType()))
                .map(cj -> new EntandoCoreComponentUsageRequest(cj.getComponentType().getAppEngineTypeName(),
                        cj.getComponentId()))
                .collect(Collectors.toList());

        List<EntandoCoreComponentUsage> usageListResponse = this.client.getComponentsUsageDetails(usageListRequest);

        Map<String, EntandoBundleComponentJobEntity> bundleComponents = bundleInstalledComponents.stream()
                .collect(Collectors.toMap(this::composeKey, c -> c));

        List<ComponentUsage> outputList = usageListResponse.stream()
                .map(ComponentUsage::fromEntandoCore)
                .flatMap(Optional::stream)
                .collect(Collectors.toList());

        outputList.forEach(componentUsage -> computeReferenceTypes(bundleComponents, componentUsage));
        return outputList;

    }

    private void computeReferenceTypes(Map<String, EntandoBundleComponentJobEntity> bundleComponents,
            ComponentUsage componentUsage) {
        componentUsage.getReferences().stream()
                .filter(Objects::nonNull)
                .forEach(ref -> {
                    if (bundleContainsRef(bundleComponents, ref)) {
                        ref.setReferenceType(ComponentReferenceType.INTERNAL);
                    } else {
                        ref.setReferenceType(ComponentReferenceType.EXTERNAL);
                        componentUsage.setHasExternal(true);
                    }
                });
    }

    private boolean bundleContainsRef(Map<String, EntandoBundleComponentJobEntity> bundleComponents,
            ComponentReference ref) {
        return bundleComponents.containsKey(composeKey(ref));
    }

    private String composeKey(ComponentReference ref) {
        if (ref == null || ref.getComponentType() == null || ref.getCode() == null) {
            log.warn("Error compose key element or code or type null ref:'{}'", ref);
            return "null-key";
        }
        return ref.getCode() + KEY_SEP + ref.getComponentType();
    }

    private String composeKey(EntandoBundleComponentJobEntity comp) {
        if (comp == null || comp.getComponentType() == null) {
            log.warn("Error compose key element or type null comp:'{}'", comp);
            return "null-key";
        }
        return comp.getComponentId() + KEY_SEP + comp.getComponentType().getTypeName();
    }

}