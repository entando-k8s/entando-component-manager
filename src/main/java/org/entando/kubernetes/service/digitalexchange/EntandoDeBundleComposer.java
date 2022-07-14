package org.entando.kubernetes.service.digitalexchange;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.entando.kubernetes.exception.EntandoComponentManagerException;
import org.entando.kubernetes.exception.EntandoValidationException;
import org.entando.kubernetes.model.bundle.BundleInfo;
import org.entando.kubernetes.model.bundle.BundleInfo.BundleGroup;
import org.entando.kubernetes.model.bundle.BundleType;
import org.entando.kubernetes.model.bundle.ComponentType;
import org.entando.kubernetes.model.bundle.EntandoBundleVersion;
import org.entando.kubernetes.model.bundle.descriptor.BundleDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.ComponentSpecDescriptor;
import org.entando.kubernetes.model.bundle.downloader.BundleDownloader;
import org.entando.kubernetes.model.bundle.downloader.BundleDownloaderFactory;
import org.entando.kubernetes.model.bundle.downloader.BundleDownloaderType.BundleDownloaderConstants;
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

    public static final String PBC_ANNOTATIONS_KEY = "pbc.entando.org/";
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
        if (ObjectUtils.isEmpty(bundleInfo.getGitRepoAddress())) {
            throw new EntandoValidationException("The received bundle url is null");
        }
        final EntandoDeBundleTag tagAsSelector = (new EntandoDeBundleTagBuilder()).withTarball(
                bundleInfo.getGitRepoAddress()).build();
        final BundleDownloader bundleDownloader = downloaderFactory.newDownloader(tagAsSelector);

        final BundleDescriptor bundleDescriptor = this.fetchBundleDescriptor(bundleDownloader,
                bundleInfo.getGitRepoAddress(), bundleInfo.getVersion());
        if (bundleDescriptor == null) {
            throw new EntandoComponentManagerException("Null bundle descriptor");
        }

        final List<String> tagList = bundleDownloader.fetchRemoteTags(bundleInfo.getGitRepoAddress());
        if (CollectionUtils.isEmpty(tagList)) {
            throw new EntandoComponentManagerException("No versions available for the received bundle");
        }

        return createEntandoDeBundle(bundleDescriptor, tagList, bundleInfo);
    }

    /**
     * fetch the bundle descriptor.
     *
     * @param bundleDownloader the bundledownloader to use for the current operation
     * @param bundleUrl        the url of the bundle of which fetching the descriptor
     * @param version          the version of the bundle of which fetching the descriptor
     * @return the fetched bundle descriptor
     */
    private BundleDescriptor fetchBundleDescriptor(BundleDownloader bundleDownloader, String bundleUrl,
            String version) {

        try {
            final String httpProtocolUrl;
            if (bundleUrl.startsWith(BundleDownloaderConstants.DOCKER_PROTOCOL)) {
                httpProtocolUrl = bundleUrl + ":" + version;
                bundleUrl = bundleUrl + ":" + version;
            } else {
                httpProtocolUrl = BundleUtilities.gitSshProtocolToHttp(bundleUrl);

            }
            ValidationFunctions.composeCommonUrlOrThrow(httpProtocolUrl,
                    "Bundle url is empty", "Bundle url is not valid");

            Path pathToDownloadedBundle = bundleDownloader.saveBundleLocally(bundleUrl);
            final BundleReader bundleReader = new BundleReader(pathToDownloadedBundle, httpProtocolUrl);
            return bundleReader.readBundleDescriptor();
        } catch (IOException e) {
            throw new EntandoComponentManagerException("Error during bundle descriptor reading");
        } finally {
            bundleDownloader.cleanTargetDirectory();
        }
    }

    private EntandoDeBundle createEntandoDeBundle(BundleDescriptor bundleDescriptor, List<String> tagList,
            BundleInfo bundleInfo) {

        final List<EntandoDeBundleTag> deBundleTags = createTagsFrom(tagList, bundleInfo.getGitRepoAddress());
        final List<String> versionList = deBundleTags.stream()
                .map(EntandoDeBundleTag::getVersion)
                .collect(Collectors.toList());

        return new EntandoDeBundleBuilder()
                .withNewMetadata()
                // FIXME  should be bundleDescriptor.getCode()
                //.withName(BundleUtilities.composeBundleIdentifier(bundleInfo.getName()))
                .withName(bundleDescriptor.getCode())
                .withLabels(createLabelsFrom(bundleDescriptor))
                .withAnnotations(createAnnotationsFrom(bundleInfo.getBundleGroups()))
                .endMetadata()
                .withNewSpec()
                .withNewDetails()
                // FIXME  should be bundleDescriptor.getName()
                //.withName(bundleDescriptor.getCode())
                .withName(bundleInfo.getName())
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

    private Map<String, String> createAnnotationsFrom(List<BundleGroup> bundleGroups) {

        return Optional.ofNullable(bundleGroups)
                .orElseGet(ArrayList::new).stream()
                .filter(g -> ObjectUtils.isNotEmpty(g.getName()))
                .map(g -> PBC_ANNOTATIONS_KEY + g.getName())
                .collect(Collectors.toMap(k -> k, v -> "true"));
    }

    /**
     * put an entry in the received map with key = labelName and value = "true" if the componentList size is > 0.
     *
     * @param labelsMap     the map of the labels
     * @param componentList the component list of which check the size
     * @param labelName     the name of the label
     */
    private void putLabelIntoMap(Map<String, String> labelsMap, List<String> componentList, String labelName) {
        if (!CollectionUtils.isEmpty(componentList)) {
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
    private List<EntandoDeBundleTag> createTagsFrom(List<String> tagList, String bundleUrl) {

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
                        .withTarball(bundleUrl)
                        .build())
                .collect(Collectors.toList());
    }
}
