package org.entando.kubernetes.service.digitalexchange.job;

import static org.assertj.core.api.Java6Assertions.assertThat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.entando.kubernetes.service.digitalexchange.BundleUtilities;
import org.entando.kubernetes.service.digitalexchange.job.PostInitConfigurationService.PostInitData;
import org.entando.kubernetes.service.digitalexchange.job.PostInitConfigurationService.PostInitItem;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
class PostInitConfigurationServiceTest {

    private PostInitConfigurationServiceImpl serviceConfigToTest;
    private static final String POST_INIT_BUNDLE_VERSION = "0.0.2";
    private static final String POST_INIT_BUNDLE_NAME = "test-bundle-entando-post-init-01";
    private static final String POST_INIT_BUNDLE_PUBLICATION_URL = "docker://docker.io/entando/post-init";
    private ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    public void setup() throws Exception {

    }

    @AfterEach
    public void teardown() {
    }

    @Test
    void postInit_errorInputConfig_ShouldNotInstall() throws Exception {
        initServiceToTest("{%/()}");
        assertThat(serviceConfigToTest.getFrequencyInSeconds()).isEqualTo(5);

        initServiceToTest(" ");
        assertThat(serviceConfigToTest.getFrequencyInSeconds()).isEqualTo(5);
        assertThat(serviceConfigToTest.getMaxAppWaitInSeconds()).isEqualTo(600);

    }

    @Test
    void isBundleOperationAllowed_shouldWork() throws Exception {
        initServiceToTest(convertConfigDataToString(convertConfigDataToString()));

        Optional<Boolean> resp = serviceConfigToTest.isEcrActionAllowed("ciccio", "test");
        assertThat(resp.isEmpty()).isTrue();

        String bundleCode = BundleUtilities.composeBundleCode(POST_INIT_BUNDLE_NAME,
                BundleUtilities.removeProtocolAndGetBundleId(POST_INIT_BUNDLE_PUBLICATION_URL));
        resp = serviceConfigToTest.isEcrActionAllowed(bundleCode, "test");
        assertThat(resp.get()).isFalse();

        PostInitData data = convertConfigDataToString();
        PostInitItem item = data.getItems().get(0);
        item.setEcrActions(new String[0]);
        initServiceToTest(convertConfigDataToString(data));
        resp = serviceConfigToTest.isEcrActionAllowed(bundleCode, "uninstall");
        assertThat(resp.get()).isFalse();

        item.setEcrActions(new String[]{"undeploy"});
        initServiceToTest(convertConfigDataToString(data));
        resp = serviceConfigToTest.isEcrActionAllowed(bundleCode, "uninstall");
        assertThat(resp.get()).isFalse();

        item.setEcrActions(new String[]{"uninstall"});
        initServiceToTest(convertConfigDataToString(data));
        resp = serviceConfigToTest.isEcrActionAllowed(bundleCode, "uninstall");
        assertThat(resp.get()).isTrue();

    }

    private void initServiceToTest(String configurationData) throws Exception {
        serviceConfigToTest = new PostInitConfigurationServiceImpl(configurationData);
        serviceConfigToTest.afterPropertiesSet();
    }

    private String convertConfigDataToString(PostInitData configData) throws JsonProcessingException {
        return mapper.writeValueAsString(configData);
    }

    private PostInitData convertConfigDataToString() {
        List<PostInitItem> items = new ArrayList<>();
        items.add(PostInitItem.builder()
                .name(POST_INIT_BUNDLE_NAME)
                .url(POST_INIT_BUNDLE_PUBLICATION_URL)
                .version(POST_INIT_BUNDLE_VERSION)
                .action("install-or-update")
                .priority(1)
                .build());
        return PostInitData.builder().frequency(3).items(items).build();

    }

}
