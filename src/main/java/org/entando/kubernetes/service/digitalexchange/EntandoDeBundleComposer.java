package org.entando.kubernetes.service.digitalexchange;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
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
import org.apache.commons.lang3.StringUtils;
import org.entando.kubernetes.client.k8ssvc.K8SServiceClient;
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

    public static final String PBC_ANNOTATIONS_KEY = "entando.org/pbc";
    private final BundleDownloaderFactory downloaderFactory;
    private final K8SServiceClient k8SServiceClient;
    private static final String MAIN_VERSION = "main";
    private ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    public EntandoDeBundleComposer(BundleDownloaderFactory downloaderFactory, K8SServiceClient k8SServiceClient) {
        this.downloaderFactory = downloaderFactory;
        this.k8SServiceClient = k8SServiceClient;
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

        final BundleDownloader bundleDownloader = downloaderFactory.newDownloader(bundleInfo.getGitRepoAddress());

        final List<String> tagList = bundleDownloader.fetchRemoteTags(bundleInfo.getGitRepoAddress());
        if (CollectionUtils.isEmpty(tagList)) {
            throw new EntandoComponentManagerException("No versions available for the received bundle");
        }

        final BundleDescriptor bundleDescriptor = this.fetchBundleDescriptor(bundleDownloader,
                bundleInfo.getGitRepoAddress(), selectVersionToFetch(tagList));
        if (bundleDescriptor == null) {
            throw new EntandoComponentManagerException("Null bundle descriptor");
        }

        return createEntandoDeBundle(bundleDescriptor, tagList, bundleInfo);
    }

    private String selectVersionToFetch(List<String> tagList) {
        if (tagList.contains(MAIN_VERSION)) {
            return MAIN_VERSION;
        } else {
            return tagList.stream()
                    .map(tag -> new EntandoBundleVersion().setVersion(tag))
                    .filter(Objects::nonNull)
                    .filter(v -> !v.isSnapshot())
                    .min(Comparator.comparing(EntandoBundleVersion::getSemVersion))
                    .map(EntandoBundleVersion::getVersion)
                    .orElseThrow(() -> new EntandoComponentManagerException("Cannot find the first bundle version"));
        }
    }

    /**
     * fetch the bundle descriptor.
     *
     * @param bundleDownloader the bundledownloader to use for the current operation
     * @param bundleUrl        the url of the bundle of which fetching the descriptor
     * @param version          the version of the bundle of which fetching the descriptor used only for DOCKER protocol
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

        final Optional<EntandoDeBundle> oldBundle = k8SServiceClient.getBundlesInObservedNamespaces(
                Optional.of(bundleInfo.getGitRepoAddress())).stream().findFirst();
        final List<EntandoDeBundleTag> deBundleTags = createTagsFrom(tagList, bundleInfo.getGitRepoAddress());
        final List<String> versionList = deBundleTags.stream()
                .map(EntandoDeBundleTag::getVersion)
                .collect(Collectors.toList());

        return new EntandoDeBundleBuilder()
                .withNewMetadata()
                .withName(retrieveMetadataName(bundleDescriptor.getCode(), oldBundle))
                .withLabels(createLabelsFrom(bundleDescriptor))
                .withAnnotations(createAnnotationsFrom(retrieveBundleGroup(bundleInfo, oldBundle)))
                .endMetadata()
                .withNewSpec()
                .withNewDetails()
                .withName(retrieveSpecName(bundleDescriptor, oldBundle))
                .withDescription(bundleDescriptor.getDescription())
                .addNewDistTag(BundleUtilities.LATEST_VERSION, getLatestSemverVersion(deBundleTags))
                .withVersions(versionList)
                .withThumbnail(retrieveThumbnail(bundleDescriptor, bundleInfo))
                .endDetails()
                .withTags(deBundleTags)
                .endSpec()
                .build();
    }

    private List<BundleGroup> retrieveBundleGroup(BundleInfo bundleInfo, Optional<EntandoDeBundle> oldBundle) {
        return Optional.ofNullable(bundleInfo.getBundleGroups())
                .orElse(oldBundle.map(b -> createBundleGroupFrom(b.getMetadata().getAnnotations()))
                        .orElse(Collections.emptyList()));
    }

    private String retrieveMetadataName(String bundleCode, Optional<EntandoDeBundle> oldBundle) {
        return oldBundle.map(b -> b.getMetadata().getName()).orElse(bundleCode);
    }

    private String retrieveSpecName(BundleDescriptor bundleDescriptor, Optional<EntandoDeBundle> oldBundle) {
        return Optional.ofNullable(bundleDescriptor.getName())
                .orElse(oldBundle.map(b -> b.getSpec().getDetails().getName()).orElse(null));
    }

    private String retrieveThumbnail(BundleDescriptor bundleDescriptor, BundleInfo bundleInfo) {
        if (StringUtils.isNotBlank(bundleDescriptor.getThumbnail())) {
            return bundleDescriptor.getThumbnail();
        } else {
            return bundleInfo.getDescriptionImage();
        }
    }

    private String getLatestSemverVersion(List<EntandoDeBundleTag> deBundleTags) {

        if (CollectionUtils.isEmpty(deBundleTags)) {
            throw new EntandoComponentManagerException("Null or empty tag list: can't determine latest version");
        }

        return deBundleTags.stream()
                .map(EntandoBundleVersion::fromEntity)
                .filter(Objects::nonNull)
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

        final List<String> pbcList = Optional.ofNullable(bundleGroups)
                .orElseGet(ArrayList::new).stream()
                .map(BundleGroup::getName)
                .filter(ObjectUtils::isNotEmpty)
                .collect(Collectors.toList());

        try {
            return Map.of(PBC_ANNOTATIONS_KEY, objectMapper.writeValueAsString(pbcList));
        } catch (JsonProcessingException e) {
            throw new EntandoComponentManagerException("An error occurred while serializing bundle's pbc names", e);
        }
    }

    private List<BundleGroup> createBundleGroupFrom(Map<String, String> annotations) {

        return Optional.ofNullable(annotations).map(m -> m.get(PBC_ANNOTATIONS_KEY)).map(s -> {
            try {
                List<String> pbcList = objectMapper.readValue(s, new TypeReference<List<String>>() {
                });
                return pbcList.stream().map(pbc -> new BundleGroup(null, pbc)).collect(Collectors.toList());
            } catch (JsonProcessingException ex) {
                log.info("Converting CR annotation:'{}' fo json caught error:'{}', set bundle groups to null",
                        PBC_ANNOTATIONS_KEY,
                        ex.getMessage());
                log.debug("Exception converting value:'{}' to json", s, ex);
                return null;
            }
        }).orElse(null);
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
                .map(tag -> new EntandoBundleVersion().setVersion(tag))
                .filter(Objects::nonNull)
                .map(semver -> new EntandoDeBundleTagBuilder()
                        .withVersion(semver.getVersion())
                        .withTarball(bundleUrl)
                        .build())
                .collect(Collectors.toList());
    }
}
