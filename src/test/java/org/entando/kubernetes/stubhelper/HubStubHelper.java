package org.entando.kubernetes.stubhelper;

import org.entando.kubernetes.client.hub.ProxiedPayload;
import org.entando.kubernetes.client.hub.domain.BundleDto;
import org.entando.kubernetes.client.hub.domain.BundleEntityDto;
import org.entando.kubernetes.client.hub.domain.BundleGroupEntityDto;
import org.entando.kubernetes.client.hub.domain.BundleGroupVersionEntityDto;
import org.entando.kubernetes.client.hub.domain.BundleGroupVersionFilteredResponseView;
import org.entando.kubernetes.client.hub.domain.BundleGroupVersionStatus;
import org.entando.kubernetes.client.hub.domain.CategoryEntityDto;
import org.entando.kubernetes.client.hub.domain.HubDescriptorVersion;
import org.entando.kubernetes.client.hub.domain.OrganisationEntityDto;
import org.entando.kubernetes.client.hub.domain.PagedContent;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpStatus;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class HubStubHelper {

    public static ProxiedPayload<PagedContent<BundleGroupVersionFilteredResponseView, BundleGroupVersionEntityDto>> stubBundleGroupVersionsProxiedPayload() {

        return ProxiedPayload.<PagedContent<BundleGroupVersionFilteredResponseView, BundleGroupVersionEntityDto>>builder()
                .status(HttpStatus.OK)
                .payload(stubBundleGroupVersionsPagedContent())
                .build();
    }

    public static PagedContent<BundleGroupVersionFilteredResponseView, BundleGroupVersionEntityDto> stubBundleGroupVersionsPagedContent() {

        return new PagedContent<>(
                List.of(stubBundleGroupVersionFilteredResponseView()),
                new PageImpl<>(List.of(stubBundleGroupVersionEntityDto())));
    }

    public static ProxiedPayload<PagedContent<BundleDto, BundleEntityDto>> stubBundleDtosProxiedPayload() {

        return ProxiedPayload.<PagedContent<BundleDto, BundleEntityDto>>builder()
                .status(HttpStatus.OK)
                .payload(stubBundleDtosPagedContent())
                .build();
    }

    public static PagedContent<BundleDto, BundleEntityDto> stubBundleDtosPagedContent() {

        return new PagedContent<>(
                List.of(stubBundleDto()),
                new PageImpl<>(List.of(stubBundleEntityDto())));
    }

    private static BundleEntityDto stubBundleEntityDto() {
        return BundleEntityDto.builder()
                .id(Long.parseLong(BUNDLE_ID))
                .name(BUNDLE_NAME)
                .descriptorVersion(DESCRIPTOR_VERSION)
                .gitRepoAddress(GIT_REPO_ADDRESS)
                .gitSrcRepoAddress(GIT_SRC_REPO_ADDRESS)
                .dependencies(DEPENDENCIES)
                .bundleGroupVersions(Set.of(stubBundleGroupVersionEntityDto()))
                .build();
    }

    private static final String BUNDLE_ID = "2677";
    private static final String BUNDLE_NAME = "Yet another bundle";
    public static final Long ID = 30L;
    public static final Long BUNDLE_GROUP_ID = 10L;
    public static final Long BUNDLE_GROUP_VERSION_ID = 100L;
    public static final Long CATALOG_ID = 70L;
    public static final String NAME = "entando bundle";
    public static final String DESCRIPTION = "my descr";
    public static final HubDescriptorVersion DESCRIPTOR_VERSION = HubDescriptorVersion.V5;
    public static final String DEPENDENCIES = "my deps";
    public static final String DESCRIPTION_IMAGE = "wonderful image";
    public static final String DOCUMENTATION_URL = "http://www.entando.com";
    public static final String GIT_REPO_ADDRESS = "http://www.github.com/entando/test";
    public static final String GIT_SRC_REPO_ADDRESS = "http://www.github.com/entando/srctest";
    public static final String VERSION = "v1.0.0";
    public static final BundleGroupVersionStatus STATUS = BundleGroupVersionStatus.PUBLISHED;
    public static final Long ORGANISATION_ID = 5L;
    public static final String ORGANISATION_NAME = "Entando org";
    public static final String ORGANISATION_DESCR = "Entando org descr";
    public static final boolean PUBLIC_CATALOG = true;
    public static final List<String> CATEGORIES = List.of("cat 1", "cat 2");
    public static final List<String> CHILDREN = List.of("child 1", "child 2");
    public static final List<String> ALL_VERSIONS = List.of("v1.0.0", "v2.0.0");
    //    public static final LocalDateTime createdAt;
//    public static final LocalDateTime lastUpdate;
    public static final String BUNDLE_GROUP_URL = "http://www.mybundle.com";
    public static final Boolean IS_EDITABLE = true;
    public static final boolean CAN_ADD_NEW_VERSION = true;
    public static final Boolean DISPLAY_CONTACT_URL = true;
    public static final String CONTACT_URL = "http://www.entando.com/contacts";



    public static BundleGroupVersionFilteredResponseView stubBundleGroupVersionFilteredResponseView() {
        return new BundleGroupVersionFilteredResponseView()
                .setBundleGroupId(BUNDLE_GROUP_ID)
                .setBundleGroupVersionId(BUNDLE_GROUP_VERSION_ID)
                .setName(NAME)
                .setDescription(DESCRIPTION)
                .setDescriptionImage(DESCRIPTION_IMAGE)
                .setDocumentationUrl(DOCUMENTATION_URL)
                .setVersion(VERSION)
                .setStatus(STATUS)
                .setOrganisationId(ORGANISATION_ID)
                .setOrganisationName(ORGANISATION_NAME)
                .setPublicCatalog(PUBLIC_CATALOG)
                .setCategories(CATEGORIES)
                .setChildren(CHILDREN)
                .setAllVersions(ALL_VERSIONS)
                .setBundleGroupUrl(BUNDLE_GROUP_URL)
                .setIsEditable(IS_EDITABLE)
                .setCanAddNewVersion(CAN_ADD_NEW_VERSION)
                .setDisplayContactUrl(DISPLAY_CONTACT_URL)
                .setContactUrl(CONTACT_URL);
    }


    public static BundleGroupVersionEntityDto stubBundleGroupVersionEntityDto() {
        BundleGroupVersionEntityDto stub = new BundleGroupVersionEntityDto();
        stub.setId(ID);
        stub.setDescription(DESCRIPTION);
        stub.setDocumentationUrl(DOCUMENTATION_URL);
        stub.setVersion(VERSION);
        stub.setDescriptionImage(DESCRIPTION_IMAGE);
        stub.setStatus(STATUS);
        stub.setDisplayContactUrl(DISPLAY_CONTACT_URL);
        stub.setContactUrl(CONTACT_URL);
        stub.setBundleGroup(stubBundleGroupEntityDto());
        stub.setBundles(Set.of(stubBundleEntityDto(1), stubBundleEntityDto(2)));
//        stub.setLastUpdated();
        return stub;
    }

    public static BundleGroupEntityDto stubBundleGroupEntityDto() {
        BundleGroupEntityDto stub = new BundleGroupEntityDto();
        stub.setId(ID);
        stub.setName(NAME);
        stub.setCatalogId(CATALOG_ID);
        stub.setPublicCatalog(PUBLIC_CATALOG);
        stub.setOrganisation(stubOrganisationEntityDto());
        stub.setCategories(Set.of(stubCategoryEntityDto(1), stubCategoryEntityDto(2)));
        return stub;
    }

    public static OrganisationEntityDto stubOrganisationEntityDto() {
        OrganisationEntityDto stub = new OrganisationEntityDto();
        stub.setId(ORGANISATION_ID);
        stub.setName(ORGANISATION_NAME);
        stub.setDescription(ORGANISATION_DESCR);
        return stub;
    }

    public static CategoryEntityDto stubCategoryEntityDto(int i) {
        return CategoryEntityDto.builder()
                .id(ORGANISATION_ID + i)
                .name(ORGANISATION_NAME + i)
                .description(ORGANISATION_DESCR + i)
                .build();
    }

    public static BundleEntityDto stubBundleEntityDto(int i) {
        return BundleEntityDto.builder()
                .id(ORGANISATION_ID + i)
                .name(ORGANISATION_NAME + i)
                .description(DESCRIPTION + i)
                .gitRepoAddress(GIT_REPO_ADDRESS + i)
                .gitSrcRepoAddress(GIT_SRC_REPO_ADDRESS + i)
                .dependencies(DEPENDENCIES + i)
                .descriptorVersion(DESCRIPTOR_VERSION)
                .build();
    }

    public static BundleDto stubBundleDto() {
        return BundleDto.builder()
                .bundleId(BUNDLE_ID)
                .name(BUNDLE_NAME)
                .description(DESCRIPTION)
                .descriptionImage(DESCRIPTION_IMAGE)
                .descriptorVersion(DESCRIPTOR_VERSION.toString())
                .gitRepoAddress(GIT_REPO_ADDRESS)
                .gitSrcRepoAddress(GIT_SRC_REPO_ADDRESS)
                .dependencies(Arrays.asList(DEPENDENCIES.split(" ")))
                .bundleGroups(Arrays.asList(BUNDLE_GROUP_ID.toString()))
                .build();
    }
}
