package org.entando.kubernetes.stubhelper;

import java.net.MalformedURLException;
import java.net.URL;
import org.entando.kubernetes.model.bundle.BundleStatus;
import org.entando.kubernetes.model.bundle.status.BundlesStatusItem;

public class BundleStatusItemStubHelper {

    public static final String ID_INSTALLED_NOT_DEPLOYED = "http://www.github.com/entando/installed_not_deployed.git";
    public static final BundleStatus STATUS_INSTALLED_NOT_DEPLOYED = BundleStatus.INSTALLED_NOT_DEPLOYED;
    public static final String INSTALLED_VERSION_INSTALLED_NOT_DEPLOYED = "v1.2.0";

    public static final String ID_INSTALLED = "http://www.github.com/entando/installed.git";
    public static final BundleStatus STATUS_INSTALLED = BundleStatus.INSTALLED;
    public static final String INSTALLED_VERSION_INSTALLED = "v1.2.0";

    public static final String ID_DEPLOYED = "http://www.github.com/entando/deployed.git";
    public static final BundleStatus STATUS_DEPLOYED = BundleStatus.DEPLOYED;
    public static final String INSTALLED_VERSION_DEPLOYED = null;

    public static final String ID_NOT_FOUND = "http://www.github.com/entando/not_found.git";
    public static final BundleStatus STATUS_NOT_FOUND = BundleStatus.NOT_FOUND;
    public static final String INSTALLED_VERSION_NOT_FOUND = null;

    public static final String ID_INVALID_REPO_URL = "http://   invalid_repo_url.git";
    public static final BundleStatus STATUS_INVALID_REPO_URL = BundleStatus.INVALID_REPO_URL;
    public static final String INSTALLED_VERSION_INVALID_REPO_URL = null;

    public static BundlesStatusItem stubBundleStatusItem() {
        return stubBundleStatusItemInstalled();
    }

    public static BundlesStatusItem stubBundleStatusItemInstalled() {
        return new BundlesStatusItem()
                .setId(ID_INSTALLED)
                .setStatus(STATUS_INSTALLED)
                .setInstalledVersion(INSTALLED_VERSION_INSTALLED);
    }

    public static BundlesStatusItem stubBundleStatusItemInstalledNotDeployed() {
        return new BundlesStatusItem()
                .setId(ID_INSTALLED_NOT_DEPLOYED)
                .setStatus(STATUS_INSTALLED_NOT_DEPLOYED)
                .setInstalledVersion(INSTALLED_VERSION_INSTALLED_NOT_DEPLOYED);
    }

    public static BundlesStatusItem stubBundleStatusItemDeployed() {
        return new BundlesStatusItem()
                .setId(ID_DEPLOYED)
                .setStatus(STATUS_DEPLOYED)
                .setInstalledVersion(INSTALLED_VERSION_DEPLOYED);
    }

    public static BundlesStatusItem stubBundleStatusItemNotFound() {
        return new BundlesStatusItem()
                .setId(ID_NOT_FOUND)
                .setStatus(STATUS_NOT_FOUND)
                .setInstalledVersion(INSTALLED_VERSION_NOT_FOUND);
    }

    public static BundlesStatusItem stubBundleStatusItemInvalidRepoUrl() {
        return new BundlesStatusItem()
                .setId(ID_INVALID_REPO_URL)
                .setStatus(STATUS_INVALID_REPO_URL)
                .setInstalledVersion(INSTALLED_VERSION_INVALID_REPO_URL);
    }

    public static URL stubURLInvalidUrl() throws MalformedURLException {
        return new URL(ID_INVALID_REPO_URL);
    }
}
