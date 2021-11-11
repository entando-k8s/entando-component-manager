package org.entando.kubernetes.stubhelper;

import java.util.Collections;
import java.util.List;
import org.entando.kubernetes.model.bundle.BundleInfo;

public class BundleInfoStubHelper {

    public static final String NAME = "my-bundle";
    public static final String DESCRIPTION = "desc";
    public static final String GIT_REPO_ADDRESS = "http://www.github.com/mybundle.git";
    public static final String DESCR_IMAGE = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAARkAAAEZCAYAAACjEFEXAAAPLElEQVR4nOzdD2yc5X3A8V";
    public static final List<String> GROUPS = List.of("46", "67");
    public static final String BUNDLE_ID = "140";
    public static final List<String> DEPENDENCIES = Collections.emptyList();

    public static BundleInfo stubBunbleInfo() {
        return new BundleInfo()
                .setName(NAME)
                .setDescription(DESCRIPTION)
                .setGitRepoAddress(GIT_REPO_ADDRESS)
                .setDescriptionImage(DESCR_IMAGE)
                .setBundleGroups(GROUPS)
                .setBundleId(BUNDLE_ID)
                .setDependencies(DEPENDENCIES);
    }
}
