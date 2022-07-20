package org.entando.kubernetes.stubhelper;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.entando.kubernetes.model.bundle.BundleInfo;

public class BundleInfoStubHelper {

    public static final String NAME = "my-bundle";
    public static final String DESCRIPTION = "desc";
    public static final String GIT_REPO_ADDRESS = "http://www.github.com/entando/mybundle.git";
    public static final String GIT_REPO_ADDRESS_8_CHARS_SHA = "77b2b10e";
    public static final String DESCR_IMAGE = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAARkAAAEZCAYAAACjEFEXAAAPLElEQVR4nOzdD2yc5X3A8V";
    public static final List<String> GROUPS_ID = List.of("46", "67");
    public static final String GROUP_NAME_1 = "group one";
    public static final String GROUP_NAME_2 = "group two";
    public static final List<String> GROUPS_NAME = List.of(GROUP_NAME_1, GROUP_NAME_2);
    public static final String PBC_ANNOTATION_VALUE = "[\"" + GROUP_NAME_1 + "\",\"" + GROUP_NAME_2 + "\"]";
    public static final String BUNDLE_ID = "140";
    public static final List<String> DEPENDENCIES = Collections.emptyList();

    public static BundleInfo stubBunbleInfo() {
        return new BundleInfo()
                .setName(NAME)
                .setDescription(DESCRIPTION)
                .setGitRepoAddress(GIT_REPO_ADDRESS)
                .setDescriptionImage(DESCR_IMAGE)
                .setBundleGroups(stubBundleGroups())
                .setBundleId(BUNDLE_ID)
                .setDependencies(DEPENDENCIES);
    }

    public static List<BundleInfo.BundleGroup> stubBundleGroups() {
        return IntStream.range(0, GROUPS_ID.size())
                .mapToObj(i -> new BundleInfo.BundleGroup(GROUPS_ID.get(i), GROUPS_NAME.get(i)))
                .collect(Collectors.toList());
    }
}
