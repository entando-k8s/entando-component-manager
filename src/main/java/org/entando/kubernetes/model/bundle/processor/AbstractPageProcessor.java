package org.entando.kubernetes.model.bundle.processor;

import static org.entando.kubernetes.service.digitalexchange.BundleUtilities.removeProtocolAndGetBundleId;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.entando.kubernetes.client.core.EntandoCoreClient;
import org.entando.kubernetes.controller.digitalexchange.job.model.InstallAction;
import org.entando.kubernetes.controller.digitalexchange.job.model.InstallPlan;
import org.entando.kubernetes.model.bundle.descriptor.ComponentSpecDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.PageDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.widget.WidgetConfigurationDescriptor;
import org.entando.kubernetes.model.bundle.installable.Installable;
import org.entando.kubernetes.model.bundle.reader.BundleReader;
import org.entando.kubernetes.model.job.EntandoBundleComponentJobEntity;
import org.entando.kubernetes.service.digitalexchange.BundleUtilities;
import org.entando.kubernetes.validator.descriptor.PageDescriptorValidator;
import org.springframework.util.CollectionUtils;

@Slf4j
public abstract class AbstractPageProcessor extends BaseComponentProcessor<PageDescriptor> {

    private final EntandoCoreClient engineService;

    private final PageDescriptorValidator descriptorValidator;

    protected AbstractPageProcessor(EntandoCoreClient engineService, PageDescriptorValidator descriptorValidator) {
        this.engineService = engineService;
        this.descriptorValidator = descriptorValidator;
    }

    @Override
    public Class<PageDescriptor> getDescriptorClass() {
        return PageDescriptor.class;
    }

    @Override
    public Optional<Function<ComponentSpecDescriptor, List<String>>> getComponentSelectionFn() {
        return Optional.of(ComponentSpecDescriptor::getPages);
    }

    private List<PageDescriptor> partialSortingOfPageDescriptorParentCodeBased(List<PageDescriptor> pageDescriptorList) {
        List<PageDescriptor> pageDescList = new LinkedList<>(pageDescriptorList);
        List<PageDescriptor> sortedPageDescriptorList = new LinkedList<>();

        List<PageDescriptor> itemsWithGlobalPrefix = pageDescList.stream()
                .filter(descriptor -> descriptor.getParentCode().startsWith(BundleUtilities.GLOBAL_PREFIX))
                .collect(Collectors.toList());

        sortedPageDescriptorList.addAll(itemsWithGlobalPrefix);
        pageDescList.removeAll(itemsWithGlobalPrefix);

        Set<String> codes = pageDescList.stream()
                .map(PageDescriptor::getCode)
                .collect(Collectors.toSet());

        List<PageDescriptor> itemsWithoutParentInList = pageDescList.stream()
                .filter(descriptor -> !codes.contains(descriptor.getParentCode()))
                .collect(Collectors.toList());

        sortedPageDescriptorList.addAll(itemsWithoutParentInList);
        pageDescList.removeAll(itemsWithoutParentInList);

        List<PageDescriptor> itemsWithParentInList = pageDescList.stream()
                .filter(descriptor -> codes.contains(descriptor.getParentCode()))
                .collect(Collectors.toList());

        sortedPageDescriptorList.addAll(itemsWithParentInList);
        pageDescList.removeAll(itemsWithParentInList);

        // remaining items
        sortedPageDescriptorList.addAll(pageDescList);

        return sortedPageDescriptorList;
    }

    @Override
    public List<Installable<PageDescriptor>> process(BundleReader bundleReader) {
        return this.process(bundleReader, InstallAction.CREATE, new InstallPlan());
    }

    @Override
    public List<Installable<PageDescriptor>> process(BundleReader bundleReader, InstallAction conflictStrategy,
            InstallPlan installPlan) {
        List<Installable<PageDescriptor>> installables = new LinkedList<>();
        List<PageDescriptor> pageDescriptorList = new LinkedList<>();
        final String bundleId = removeProtocolAndGetBundleId(bundleReader.getBundleUrl());

        try {
            final List<String> descriptorList = getDescriptorList(bundleReader);
            for (String fileName : descriptorList) {
                PageDescriptor pageDescriptor = bundleReader.readDescriptorFile(fileName, PageDescriptor.class);
                replaceBundleIdPlaceholder(bundleId, pageDescriptor);
                this.descriptorValidator.validateOrThrow(pageDescriptor);
                this.composeAndSetCode(pageDescriptor, bundleReader);
                Optional.ofNullable(pageDescriptor.getWidgets()).ifPresent(widgets
                        -> widgets.stream().forEach(wd -> this.composeAndSetWidgetCode(wd, pageDescriptor, bundleReader))
                );
                pageDescriptorList.add(pageDescriptor);
            }

            List<PageDescriptor> sortedPageDescriptorList = this.partialSortingOfPageDescriptorParentCodeBased(pageDescriptorList);
            for (PageDescriptor pageDescriptor : sortedPageDescriptorList) {
                InstallAction action = extractInstallAction(pageDescriptor.getCode(), conflictStrategy, installPlan);
                installables.add(this.getInstallable(pageDescriptor, action));
            }

        } catch (IOException e) {
            throw makeMeaningfulException(e);
        }
        return installables;

    }

    @Override
    public List<Installable<PageDescriptor>> process(List<EntandoBundleComponentJobEntity> components) {
        return components.stream()
                .filter(c -> c.getComponentType() == getSupportedComponentType())
                .map(c -> this.getInstallable(this.buildDescriptorFromComponentJob(c), c.getAction()))
                .collect(Collectors.toList());
    }

    protected abstract Installable<PageDescriptor> getInstallable(PageDescriptor pageDescriptor, InstallAction action);

    @Override
    public PageDescriptor buildDescriptorFromComponentJob(EntandoBundleComponentJobEntity component) {
        return PageDescriptor.builder()
                .code(component.getComponentId())
                .build();
    }

    protected void composeAndSetCode(PageDescriptor pageDescriptor, BundleReader bundleReader) {
        if (!pageDescriptor.isVersion1()) {
            // set the code and the parentCode
            if (StringUtils.isBlank(pageDescriptor.getCode())) {
                final String pageCode = BundleUtilities.composeDescriptorCode(pageDescriptor.getCode(),
                        pageDescriptor.getName(), pageDescriptor, bundleReader.getBundleUrl(), "_");
                pageDescriptor.setCode(pageCode);
            }
            String parentName = pageDescriptor.getParentName();
            if (!StringUtils.isBlank(parentName)) {
                String parentPageCode = null;
                if (parentName.startsWith(BundleUtilities.GLOBAL_PREFIX)) {
                    parentPageCode = parentName.substring(BundleUtilities.GLOBAL_PREFIX.length());
                } else {
                    parentPageCode = BundleUtilities.composeDescriptorCode(pageDescriptor.getParentCode(),
                            pageDescriptor.getParentName(), pageDescriptor, bundleReader.getBundleUrl(), "_");
                }
                pageDescriptor.setParentCode(parentPageCode);
            }
        }
    }

    private void composeAndSetWidgetCode(WidgetConfigurationDescriptor widgetDescriptor, PageDescriptor pageDescriptor, BundleReader bundleReader) {
        if (!pageDescriptor.isVersion1() && StringUtils.isBlank(widgetDescriptor.getCode())) {
            // set the code
            String widgetCode = null;
            String widgetName = widgetDescriptor.getName();
            if (widgetName.startsWith(BundleUtilities.GLOBAL_PREFIX)) {
                widgetCode = widgetName.substring(BundleUtilities.GLOBAL_PREFIX.length());
            } else {
                widgetCode = BundleUtilities.composeDescriptorCode(widgetDescriptor.getCode(),
                    widgetDescriptor.getName(), pageDescriptor, bundleReader.getBundleUrl());
            }
            widgetDescriptor.setCode(widgetCode);
        }
    }

    protected EntandoCoreClient getEngineService() {
        return engineService;
    }

    private void replaceBundleIdPlaceholder(String bundleId, PageDescriptor descriptor) {

        super.applyBundleIdPlaceholderReplacement(bundleId, descriptor::getCode, descriptor::setCode);
        super.applyBundleIdPlaceholderReplacement(bundleId, descriptor::getParentCode, descriptor::setParentCode);
        super.applyBundleIdPlaceholderReplacement(bundleId, descriptor::getOwnerGroup, descriptor::setOwnerGroup);

        if (CollectionUtils.isEmpty(descriptor.getJoinGroups())) {
            return;
        }

        final List<String> joinGroups = descriptor.getJoinGroups().stream()
                .map(gr ->
                        super.replaceBundleIdPlaceholder(gr, bundleId))
                .collect(Collectors.toList());

        descriptor.setJoinGroups(joinGroups);
    }
}
