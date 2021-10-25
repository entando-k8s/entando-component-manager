package org.entando.kubernetes.assertionhelper;

import static org.hamcrest.core.Is.is;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import java.util.List;
import java.util.Optional;
import org.entando.kubernetes.model.bundle.status.BundlesStatusItem;
import org.hamcrest.core.IsNull;
import org.springframework.test.web.servlet.ResultActions;

public class BundleStatusItemAssertionHelper {

    public static final String BUNDLE_STATUSES_BASE_JSON_PATH = "$.payload.bundlesStatuses.";

    public static void assertOnBundlesStatusItemList(ResultActions result,
            List<BundlesStatusItem> bundlesStatusItemList) throws Exception {

        for (int i = 0; i < bundlesStatusItemList.size(); i++) {
            assertOnBundlesStatusItem(result, i, bundlesStatusItemList.get(i));
        }
    }

    public static void assertOnBundlesStatusItem(ResultActions result, Integer index,
            BundlesStatusItem bundlesStatusItem)
            throws Exception {

        String baseJsonPath = Optional.ofNullable(index)
                .map(i -> BUNDLE_STATUSES_BASE_JSON_PATH + "[" + i + "].")
                .orElse(BUNDLE_STATUSES_BASE_JSON_PATH);

        result.andExpect(jsonPath(baseJsonPath + "id", is(bundlesStatusItem.getId())))
                .andExpect(jsonPath(baseJsonPath + "status", is(bundlesStatusItem.getStatus().getStatus())));

        if (bundlesStatusItem.getInstalledVersion() == null) {
            result.andExpect(jsonPath(baseJsonPath + "installedVersion", IsNull.nullValue()));
        } else {
            result.andExpect(jsonPath(baseJsonPath + "installedVersion", is(bundlesStatusItem.getInstalledVersion())));
        }
    }
}
