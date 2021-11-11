package org.entando.kubernetes.assertionhelper;

import static org.hamcrest.core.Is.is;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import java.util.List;
import java.util.Optional;
import org.entando.kubernetes.model.bundle.status.BundlesStatusItem;
import org.hamcrest.core.IsNull;
import org.springframework.test.web.servlet.ResultActions;

public class BundleStatusItemAssertionHelper {

    public static final String BUNDLE_STATUSES_BASE_JSON_PATH =
            SimpleRestResponseAssertionHelper.BUNDLE_STATUSES_BASE_JSON_PATH + "bundlesStatuses.";

    public static void assertOnBundlesStatusItemList(ResultActions result,
            List<BundlesStatusItem> bundlesStatusItemList) throws Exception {

        for (int i = 0; i < bundlesStatusItemList.size(); i++) {
            assertOnBundlesStatusItem(result, BUNDLE_STATUSES_BASE_JSON_PATH, i, bundlesStatusItemList.get(i));
        }
    }

    public static void assertOnBundlesStatusItem(ResultActions result, String startingPath, Integer index,
            BundlesStatusItem bundlesStatusItem)
            throws Exception {

        String baseJsonPath;
        if (index == null) {
            baseJsonPath = startingPath;
        } else {
            baseJsonPath = Optional.ofNullable(index)
                    .map(i -> startingPath + "[" + i + "].")
                    .orElse(startingPath);
        }

        result.andExpect(jsonPath(baseJsonPath + "id", is(bundlesStatusItem.getId())))
                .andExpect(jsonPath(baseJsonPath + "status", is(bundlesStatusItem.getStatus().getStatus())));

        if (bundlesStatusItem.getInstalledVersion() == null) {
            result.andExpect(jsonPath(baseJsonPath + "installedVersion", IsNull.nullValue()));
        } else {
            result.andExpect(jsonPath(baseJsonPath + "installedVersion", is(bundlesStatusItem.getInstalledVersion())));
        }
    }
}
