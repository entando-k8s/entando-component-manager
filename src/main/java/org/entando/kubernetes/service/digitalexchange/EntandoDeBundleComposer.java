package org.entando.kubernetes.service.digitalexchange;

import com.github.zafarkhaja.semver.Version;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import liquibase.pro.packaged.E;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.entando.kubernetes.exception.EntandoComponentManagerException;
import org.entando.kubernetes.model.bundle.BundleInfo;
import org.entando.kubernetes.model.bundle.BundleType;
import org.entando.kubernetes.model.bundle.ComponentType;
import org.entando.kubernetes.model.bundle.EntandoBundleVersion;
import org.entando.kubernetes.model.bundle.descriptor.BundleDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.ComponentSpecDescriptor;
import org.entando.kubernetes.model.bundle.downloader.BundleDownloader;
import org.entando.kubernetes.model.bundle.downloader.BundleDownloaderFactory;
import org.entando.kubernetes.model.bundle.reader.BundleReader;
import org.entando.kubernetes.model.debundle.EntandoDeBundle;
import org.entando.kubernetes.model.debundle.EntandoDeBundleBuilder;
import org.entando.kubernetes.model.debundle.EntandoDeBundleTag;
import org.entando.kubernetes.model.debundle.EntandoDeBundleTagBuilder;
import org.entando.kubernetes.validator.ValidationFunctions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class EntandoDeBundleComposer {

    private final BundleDownloaderFactory downloaderFactory;

    @Autowired
    public EntandoDeBundleComposer(BundleDownloaderFactory downloaderFactory) {
        this.downloaderFactory = downloaderFactory;
    }

    /**
     * starting by the received BundleInfo, fetch data required to compose the corresponding  K8S custom resource
     * EntandoDeBundle and return it.
     *
     * @param bundleInfo the BundleInfo from which get info required to fetch required data
     * @return the composed EntandoDeBundle
     */
    public EntandoDeBundle composeEntandoDeBundle(BundleInfo bundleInfo) {

        if (bundleInfo == null) {
            throw new EntandoComponentManagerException("The received BundleInfo is null");
        }

        final URL bundleUrl = ValidationFunctions.composeUrlOrThrow(bundleInfo.getGitRepoAddress(),
                "Bundle url is empty", "Bundle url is not valid");

        final BundleDownloader bundleDownloader = downloaderFactory.newDownloader();

        final BundleDescriptor bundleDescriptor = this.fetchBundleDescriptor(bundleDownloader, bundleUrl);
        if (bundleDescriptor == null) {
            throw new EntandoComponentManagerException("Null bundle descriptor");
        }

        final List<String> tagList = bundleDownloader.fetchRemoteTags(bundleUrl);
        if (CollectionUtils.isEmpty(tagList)) {
            throw new EntandoComponentManagerException("No versions available for the received bundle");
        }

        return createEntandoDeBundle(bundleDescriptor, tagList, bundleUrl, bundleInfo);
    }

    /**
     * fetch the bundle descriptor.
     *
     * @param bundleDownloader the bundledownloader to use for the current operation
     * @param bundleUrl        the url of the bundle of which fetching the descriptor
     * @return
     */
    private BundleDescriptor fetchBundleDescriptor(BundleDownloader bundleDownloader, URL bundleUrl) {

        try {
            Path pathToDownloadedBundle = bundleDownloader.saveBundleLocally(bundleUrl);
            final BundleReader bundleReader = new BundleReader(pathToDownloadedBundle);
            return bundleReader.readBundleDescriptor();
        } catch (IOException e) {
            throw new EntandoComponentManagerException("Error during bundle descriptor reading");
        } finally {
            bundleDownloader.cleanTargetDirectory();
        }
    }

    private EntandoDeBundle createEntandoDeBundle(BundleDescriptor bundleDescriptor, List<String> tagList,
            URL bundleUrl, BundleInfo bundleInfo) {

        final List<EntandoDeBundleTag> deBundleTags = createTagsFrom(tagList, bundleUrl);
        final List<String> versionList = deBundleTags.stream()
                .map(EntandoDeBundleTag::getVersion)
                .collect(Collectors.toList());

        return new EntandoDeBundleBuilder()
                .withNewMetadata()
                .withName(bundleDescriptor.getCode())
                .withLabels(createLabelsFrom(bundleDescriptor))
                .endMetadata()
                .withNewSpec()
                .withNewDetails()
                .withName(bundleDescriptor.getCode())
                .withDescription(bundleDescriptor.getDescription())
                .addNewDistTag(BundleUtilities.LATEST_VERSION, getLatestSemverVersion(deBundleTags))
                .withVersions(versionList)
                .withThumbnail(bundleInfo.getDescriptionImage())
                // TODO add thumbnail
                .endDetails()
                .withTags(deBundleTags)
                .endSpec()
                .build();
    }


    private String getLatestSemverVersion(List<EntandoDeBundleTag> deBundleTags) {

        if (CollectionUtils.isEmpty(deBundleTags)) {
            throw new EntandoComponentManagerException("Null or empty tag list: can't determine latest version");
        }

        return deBundleTags.stream()
                .map(EntandoBundleVersion::fromEntity)
                .max(Comparator.comparing(EntandoBundleVersion::getSemVersion))
                .map(EntandoBundleVersion::getVersion)
                .orElseThrow(() -> new EntandoComponentManagerException("Cannot find the latest bundle version"));
    }

    private Map<String, String> createLabelsFrom(BundleDescriptor bundleDescriptor) {

        Map<String, String> labelsMap = new HashMap<>();

        final ComponentSpecDescriptor components = bundleDescriptor.getComponents();
        putLabelIntoMap(labelsMap, components.getPlugins(), ComponentType.PLUGIN.getTypeName());
        putLabelIntoMap(labelsMap, components.getWidgets(), ComponentType.WIDGET.getTypeName());
        putLabelIntoMap(labelsMap, components.getFragments(), ComponentType.FRAGMENT.getTypeName());
        putLabelIntoMap(labelsMap, components.getCategories(), ComponentType.CATEGORY.getTypeName());
        putLabelIntoMap(labelsMap, components.getPages(), ComponentType.PAGE.getTypeName());
        putLabelIntoMap(labelsMap, components.getPageTemplates(), ComponentType.PAGE_TEMPLATE.getTypeName());
        putLabelIntoMap(labelsMap, components.getContentTypes(), ComponentType.CONTENT_TYPE.getTypeName());
        putLabelIntoMap(labelsMap, components.getContentTemplates(), ComponentType.CONTENT_TEMPLATE.getTypeName());
        putLabelIntoMap(labelsMap, components.getContents(), ComponentType.CONTENT.getTypeName());
        putLabelIntoMap(labelsMap, components.getAssets(), ComponentType.ASSET.getTypeName());
        putLabelIntoMap(labelsMap, components.getGroups(), ComponentType.GROUP.getTypeName());
        putLabelIntoMap(labelsMap, components.getLabels(), ComponentType.LABEL.getTypeName());
        putLabelIntoMap(labelsMap, components.getLanguages(), ComponentType.LANGUAGE.getTypeName());

        labelsMap.put("bundle-type", BundleType.STANDARD_BUNDLE.getType());

        return labelsMap;
    }

    /**
     * put an entry in the received map with key = labelName and value = "true" if the componentList size is > 0.
     *
     * @param labelsMap     the map of the labels
     * @param componentList the component list of which check the size
     * @param labelName     the name of the label
     */
    private void putLabelIntoMap(Map<String, String> labelsMap, List<String> componentList, String labelName) {
        if (! CollectionUtils.isEmpty(componentList)) {
            labelsMap.put(labelName, "true");
        }
    }

    /**
     * create the list of EntandoDeBundleTag corresponding to the received arguments.
     *
     * @param tagList   the list of the tags of the repository
     * @param bundleUrl the repository URL
     * @return the List of EntandoDeBundleTag corresponding to the repository tags
     */
    private List<EntandoDeBundleTag> createTagsFrom(List<String> tagList, URL bundleUrl) {

        return tagList.stream()
                .map(tag -> {
                    try {
                        return new EntandoBundleVersion().setVersion(tag);
                    } catch (Exception e) {
                        log.error("Tag {} is not semver compliant. Ignoring it.", tag);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .map(semver -> new EntandoDeBundleTagBuilder()
                        .withVersion(semver.getVersion())
                        .withTarball(bundleUrl.toString())
                        .build())
                .collect(Collectors.toList());
    }
}
