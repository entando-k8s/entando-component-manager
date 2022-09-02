package org.entando.kubernetes.model.bundle.processor;

import static org.entando.kubernetes.model.bundle.descriptor.widget.WidgetDescriptor.TYPE_WIDGET_CONFIG;
import static org.entando.kubernetes.service.digitalexchange.templating.WidgetTemplateGeneratorServiceImpl.CSS_TYPE;
import static org.entando.kubernetes.service.digitalexchange.templating.WidgetTemplateGeneratorServiceImpl.JS_TYPE;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.entando.kubernetes.client.core.EntandoCoreClient;
import org.entando.kubernetes.controller.digitalexchange.job.model.InstallAction;
import org.entando.kubernetes.controller.digitalexchange.job.model.InstallPlan;
import org.entando.kubernetes.exception.EntandoComponentManagerException;
import org.entando.kubernetes.model.bundle.BundleProperty;
import org.entando.kubernetes.model.bundle.ComponentType;
import org.entando.kubernetes.model.bundle.descriptor.ComponentSpecDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.widget.WidgetDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.widget.WidgetDescriptor.ApiClaim;
import org.entando.kubernetes.model.bundle.descriptor.widget.WidgetDescriptor.ConfigUi;
import org.entando.kubernetes.model.bundle.descriptor.widget.WidgetDescriptor.DescriptorMetadata;
import org.entando.kubernetes.model.bundle.installable.Installable;
import org.entando.kubernetes.model.bundle.installable.WidgetInstallable;
import org.entando.kubernetes.model.bundle.reader.BundleReader;
import org.entando.kubernetes.model.bundle.reportable.EntandoEngineReportableProcessor;
import org.entando.kubernetes.model.job.EntandoBundleComponentJobEntity;
import org.entando.kubernetes.repository.ComponentDataRepository;
import org.entando.kubernetes.service.digitalexchange.BundleUtilities;
import org.entando.kubernetes.service.digitalexchange.templating.WidgetTemplateGeneratorService;
import org.entando.kubernetes.validator.descriptor.WidgetDescriptorValidator;
import org.springframework.stereotype.Service;

/**
 * Processor to create Widgets, can handle descriptors with custom UI embedded or a separate custom UI file.
 *
 * @author Sergio Marcelino
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WidgetProcessor extends BaseComponentProcessor<WidgetDescriptor> implements
        EntandoEngineReportableProcessor {

    public static final String EXT_API_CLAIM_ERROR = "External apiClaim \"%s\" cannot be satisfied "
            + "because there is no service on the system with name \"%s\" and bundleId \"%s\"";
    public static final String INT_API_CLAIM_ERROR = "Internal apiClaim \"%s\" cannot be satisfied "
            + "because there no service with name \"%s\", declared in the same bundle";
    private final ComponentDataRepository componentDataRepository;
    private final EntandoCoreClient engineService;
    @Getter
    private final WidgetTemplateGeneratorService templateGeneratorService;
    private final WidgetDescriptorValidator descriptorValidator;
    @Setter
    private Map<String, String> pluginIngressPathMap;
    private static final ObjectMapper jsonMapper = new ObjectMapper();


    /**
     * Map of descriptors of type widgetConfig.
     * used to recover information about the configWidgets when processing a widget
     */
    @Setter
    private Map<String, WidgetDescriptor> widgetConfigDescriptorsMap;

    @Override
    public ComponentType getSupportedComponentType() {
        return ComponentType.WIDGET;
    }

    @Override
    public Class<WidgetDescriptor> getDescriptorClass() {
        return WidgetDescriptor.class;
    }

    @Override
    public Optional<Function<ComponentSpecDescriptor, List<String>>> getComponentSelectionFn() {
        return Optional.of(ComponentSpecDescriptor::getWidgets);
    }

    @Override
    public List<Installable<WidgetDescriptor>> process(BundleReader bundleReader) {
        return this.process(bundleReader, InstallAction.CREATE, new InstallPlan());
    }

    @Override
    public List<Installable<WidgetDescriptor>> process(BundleReader bundleReader, InstallAction conflictStrategy,
            InstallPlan installPlan) {

        final List<Installable<WidgetDescriptor>> installables = new LinkedList<>();

        try {
            final List<String> descriptorList = getDescriptorList(bundleReader);

            for (final String fileName : descriptorList) {
                final WidgetDescriptor widgetDescriptor = makeWidgetDescriptorFromFile(
                        bundleReader, fileName, pluginIngressPathMap
                );

                validateApiClaims(widgetDescriptor.getApiClaims());

                composeAndSetCode(widgetDescriptor, bundleReader);
                composeAndSetParentCode(widgetDescriptor, bundleReader);
                composeAndSetCustomUi(widgetDescriptor, fileName, bundleReader);
                composeAndSetConfigUi(widgetDescriptor, bundleReader);
                if (WidgetDescriptor.TYPE_WIDGET_APPBUILDER.equals(widgetDescriptor.getType())) {
                    composeAndSetAppBuilderMetadata(widgetDescriptor, bundleReader, fileName, pluginIngressPathMap);
                }

                InstallAction action = extractInstallAction(widgetDescriptor.getCode(), conflictStrategy, installPlan);
                installables.add(
                        new WidgetInstallable(engineService, widgetDescriptor, action, componentDataRepository));
            }

        } catch (IOException e) {
            throw makeMeaningfulException(e);
        }

        return installables;
    }


    @Override
    public List<Installable<WidgetDescriptor>> process(List<EntandoBundleComponentJobEntity> components) {
        return components.stream()
                .filter(c -> c.getComponentType() == getSupportedComponentType())
                .map(c -> new WidgetInstallable(engineService, this.buildDescriptorFromComponentJob(c), c.getAction(),
                        componentDataRepository))
                .collect(Collectors.toList());
    }

    private void validateApiClaims(List<ApiClaim> apiClaims) {
        if (apiClaims != null) {
            for (var apiClaim : apiClaims) {
                if (apiClaim.getType().equals(WidgetDescriptor.ApiClaim.INTERNAL_API)) {
                    if (!checkInternalApiClaim(apiClaim)) {
                        throw new EntandoComponentManagerException(String.format(INT_API_CLAIM_ERROR,
                                apiClaim.getName(), apiClaim.getPluginName()
                        ));
                    }
                } else {
                    if (!templateGeneratorService.checkApiClaim(apiClaim, apiClaim.getBundleId())) {
                        throw new EntandoComponentManagerException(String.format(EXT_API_CLAIM_ERROR,
                                apiClaim.getName(), apiClaim.getPluginName(), apiClaim.getBundleId()
                        ));
                    }
                }
            }
        }
    }

    private boolean checkInternalApiClaim(ApiClaim apiClaim) {
        return pluginIngressPathMap != null && pluginIngressPathMap.keySet().stream()
                .anyMatch(pluginName -> pluginName.equals(apiClaim.getPluginName()));
    }

    public List<Installable<WidgetDescriptor>> collectConfigWidgets(BundleReader bundleReader,
            InstallAction conflictStrategy,
            InstallPlan installPlan) {

        final var res = new LinkedList<Installable<WidgetDescriptor>>();

        try {
            final var descriptorList = getDescriptorList(bundleReader);

            for (final String fileName : descriptorList) {
                final WidgetDescriptor widgetDescriptor =
                        makeWidgetDescriptorFromFile(bundleReader, fileName, pluginIngressPathMap);
                if (widgetDescriptor.getType().equals(TYPE_WIDGET_CONFIG)) {
                    InstallAction action = extractInstallAction(widgetDescriptor.getCode(), conflictStrategy,
                            installPlan);
                    res.add(new WidgetInstallable(engineService, widgetDescriptor, action, componentDataRepository));
                }
            }
        } catch (IOException e) {
            throw makeMeaningfulException(e);
        }

        return res;
    }

    private WidgetDescriptor makeWidgetDescriptorFromFile(BundleReader bundleReader, String fileName,
            Map<String, String> pluginIngressPathMap) {
        var widgetDescriptor = bundleReader.readDescriptorFileNg(fileName, WidgetDescriptor.class);
        final String bundleId = BundleUtilities.removeProtocolAndGetBundleId(bundleReader.getBundleUrl());
        widgetDescriptor.applyFallbacks();
        widgetDescriptor.setDescriptorMetadata(
                DescriptorMetadata.builder().pluginIngressPathMap(pluginIngressPathMap).filename(fileName).bundleCode(
                                bundleReader.getCodeNg()).bundleId(bundleId).templateGeneratorService(templateGeneratorService)
                        .build());
        descriptorValidator.validateOrThrow(widgetDescriptor);
        return widgetDescriptor;
    }

    /**
     * depending of descriptor version => read or generate the ftl custom ui.
     *
     * @param widgetDescriptor the widget descriptor on which operate on
     * @param fileName         the filename with path of the widget descriptor
     * @param bundleReader     the bundle reader used to access the bundle files
     * @throws IOException if any file access error occurs
     */
    private void composeAndSetCustomUi(WidgetDescriptor widgetDescriptor, String fileName, BundleReader bundleReader)
            throws IOException {
        if (!StringUtils.isBlank(widgetDescriptor.getCustomUi())) {
            return;
        }
        if (!StringUtils.isBlank(widgetDescriptor.getCustomUiPath())) {
            String widgetUiPath = getRelativePath(fileName, widgetDescriptor.getCustomUiPath());
            widgetDescriptor.setCustomUi(bundleReader.readFileAsString(widgetUiPath));
        }
        if (!widgetDescriptor.isVersion1()) {
            String ftl = templateGeneratorService.generateWidgetTemplate(fileName, widgetDescriptor, bundleReader);
            widgetDescriptor.setCustomUi(ftl);
        }
    }

    /**
     * Sets the data related to the widget configUi by looking up to the descriptor referenced by configMfe.
     * @param widgetDescriptor the widget descriptor on which operate on
     * @param bundleReader     the bundle reader used to access the bundle files
     */
    private void composeAndSetConfigUi(WidgetDescriptor widgetDescriptor, BundleReader bundleReader) {
        if (widgetDescriptor.isVersion1()) {
            return;
        }
        if (widgetDescriptor.getType().equals(TYPE_WIDGET_CONFIG)) {
            return;
        }
        String configMfe = widgetDescriptor.getConfigMfe();
        if (configMfe == null) {
            return;
        }

        if (widgetConfigDescriptorsMap != null && widgetConfigDescriptorsMap.containsKey(configMfe)) {
            WidgetDescriptor configWidgetDescriptor = widgetConfigDescriptorsMap.get(configMfe);

            widgetDescriptor.setConfigUi(new ConfigUi(
                    configWidgetDescriptor.getCustomElement(),
                    collectResourcesPaths(
                            configWidgetDescriptor.getDescriptorMetadata().getFilename(),
                            bundleReader)
            ));
        } else {
            log.warn("Unable to find referenced configWidget \"{}\"", configMfe);
        }
    }

    /**
     * Compose and set useful data for AppBuilder as widget metadata in the descriptor.
     */
    private void composeAndSetAppBuilderMetadata(WidgetDescriptor descriptor, BundleReader bundleReader,
            String fileName,
            Map<String, String> pluginIngressPathMap) {
        String[] assets = collectResourcesPaths(descriptor.getDescriptorMetadata().getFilename(),
                bundleReader).toArray(new String[0]);

        descriptor.setDescriptorMetadata(
                new DescriptorMetadata(pluginIngressPathMap, fileName, bundleReader.getCodeNg(), assets,
                        null, bundleReader.calculateBundleId(), templateGeneratorService));

    }

    protected List<String> collectResourcesPaths(
            String descriptorFileName,
            BundleReader bundleReader) {
        //~
        final String widgetFolder = FilenameUtils.removeExtension(descriptorFileName);
        final String bundleId = BundleUtilities.removeProtocolAndGetBundleId(bundleReader.getBundleUrl());

        var resources = new ArrayList<String>();
        resources.addAll(bundleReader.getWidgetResourcesOfType(widgetFolder, JS_TYPE));
        resources.addAll(bundleReader.getWidgetResourcesOfType(widgetFolder, CSS_TYPE));

        return resources.stream().map(file -> {
            try {
                return BundleUtilities.buildFullBundleResourcePath(
                        bundleReader,
                        BundleProperty.WIDGET_FOLDER_PATH,
                        file,
                        bundleId);
            } catch (IOException e) {
                throw new EntandoComponentManagerException(e);
            }
        }).collect(Collectors.toList());
    }

    /**
     * compose and set the widget code in the descriptor.
     */
    private void composeAndSetCode(WidgetDescriptor widgetDescriptor, BundleReader bundleReader) {
        if (! widgetDescriptor.isVersion1()) {
            // set the code
            String widgetCode = null;
            String widgetName = widgetDescriptor.getName();
            if (widgetName.startsWith(BundleUtilities.GLOBAL_PREFIX)) {
                widgetCode = widgetName.substring(BundleUtilities.GLOBAL_PREFIX.length());
            } else {
                widgetCode = BundleUtilities.composeDescriptorCode(widgetDescriptor.getCode(),
                    widgetDescriptor.getName(), widgetDescriptor, bundleReader.getBundleUrl());
            }
            widgetDescriptor.setCode(widgetCode);
        }
    }

    /**
     * compose and set the widget parent code in the descriptor.
     */
    private void composeAndSetParentCode(WidgetDescriptor descriptor, BundleReader bundleReader) {
        // set the code
        if (!ObjectUtils.isEmpty(descriptor.getParentName()) && descriptor.getParentName().startsWith(BundleUtilities.GLOBAL_PREFIX)) {
            descriptor.setParentCode(descriptor.getParentName().substring(BundleUtilities.GLOBAL_PREFIX.length()));
        } else if (ObjectUtils.isEmpty(descriptor.getParentCode())
                && !ObjectUtils.isEmpty(descriptor.getParentName())) {
            descriptor.setParentCode(descriptor.getParentName() + "-" + bundleReader.calculateBundleId());
        }
    }

    @Override
    public WidgetDescriptor buildDescriptorFromComponentJob(EntandoBundleComponentJobEntity component) {
        return WidgetDescriptor.builder()
                .code(component.getComponentId())
                .build();
    }

    /**
     * reads the keys of the components from the descriptor identified by the received filename.
     *
     * @param bundleReader the bundler reader to use in order to read the bundle
     * @param fileName     the filename identifying the descriptor file to read
     * @return the list of the keys of the components read from the descriptor
     */
    @Override
    public List<String> readDescriptorKeys(BundleReader bundleReader, String fileName,
            ComponentProcessor<?> componentProcessor) {
        try {
            WidgetDescriptor widgetDescriptor =
                    (WidgetDescriptor) bundleReader.readDescriptorFile(fileName,
                            componentProcessor.getDescriptorClass());
            widgetDescriptor.applyFallbacks();

            composeAndSetCode(widgetDescriptor, bundleReader);

            return List.of(widgetDescriptor.getComponentKey().getKey());
        } catch (IOException e) {
            throw new EntandoComponentManagerException(String.format(
                    "Error parsing content type %s from widget descriptor %s",
                    componentProcessor.getSupportedComponentType(), fileName), e);
        }
    }
}
