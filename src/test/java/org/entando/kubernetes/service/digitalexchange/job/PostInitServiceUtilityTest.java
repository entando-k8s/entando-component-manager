package org.entando.kubernetes.service.digitalexchange.job;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.entando.kubernetes.exception.digitalexchange.InvalidBundleException;
import org.entando.kubernetes.service.digitalexchange.job.PostInitConfigurationServiceImpl.PostInitItem;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
class PostInitServiceUtilityTest {


    @BeforeEach
    public void setup() throws Exception {

    }

    @AfterEach
    public void teardown() {

    }

    @Test
    void calculateBundleCodeFromPostInitItem_ShouldThrowError() {
        // bundle name more than 200
        StringBuilder testName = new StringBuilder("testname");
        int i = 0;
        while (i <= 200) {
            testName.append("a");
            i++;
        }
        final PostInitItem item1 = PostInitItem.builder().name(testName.toString()).build();
        assertThrows(InvalidBundleException.class, () -> PostInitServiceUtility.calculateBundleCode(item1));

        // bundle name no valid char
        final String testCharNotValid = "&%$£";
        final PostInitItem item2 = PostInitItem.builder().name(testCharNotValid).build();
        assertThrows(InvalidBundleException.class, () -> PostInitServiceUtility.calculateBundleCode(item2));

        // bundle name no valid start char
        final String testStartCharNotValid = "-testname";
        final PostInitItem item3 = PostInitItem.builder().name(testStartCharNotValid).build();
        assertThrows(InvalidBundleException.class, () -> PostInitServiceUtility.calculateBundleCode(item3));
    }

    @Test
    void calculateBundleCodeFromPostInitItem_ShouldBeOk() {
        final String bundleName = "jeff-bundle";
        final String bundleCode = "jeff-bundle-aae34d0e";
        final String bundleUrl = "docker://registry.hub.docker.com/cecchisandrone/jeff-bundle";

        assertThat(PostInitServiceUtility.calculateBundleCode(PostInitItem.builder()
                .name(bundleName).url(bundleUrl).build())).isEqualTo(bundleCode);
    }

}
