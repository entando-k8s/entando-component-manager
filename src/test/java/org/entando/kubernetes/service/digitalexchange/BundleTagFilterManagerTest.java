package org.entando.kubernetes.service.digitalexchange;

import static org.assertj.core.api.Java6Assertions.assertThat;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
class BundleTagFilterManagerTest {

    private final List<String> invalidTags = List.of(
            "main",
            "INVALID",
            "v2.0.0-",
            "3.0.0-",
            "-3.1.1",
            "a3.0.0-fix.1");
    private final List<String> prodTags = List.of(
            "2.0.0",
            "v2.0.0",
            "v3.0.0",
            "v3.1.1",
            "3.0.0-fix.1",
            "3.0.0-patch.1",
            "v3.0.0-fix.1",
            "v3.0.0-patch.1");

    private final List<String> devTags = List.of(
            "3.0.0-SNAPSHOT",
            "3.0.0-EHUB-234-PR-34",
            "v3.0.0-SNAPSHOT",
            "v3.0.0-EHUB-234-PR-34",
            "v4.0.0-MY-CUSTOM-TAG");

    private final List<String> prodAndDevTags = Stream.concat(prodTags.stream(), devTags.stream())
            .collect(Collectors.toList());

    private final List<String> allTags = Stream.concat(Stream.concat(invalidTags.stream(), prodTags.stream()), devTags.stream())
            .collect(Collectors.toList());

    @Test
    void shouldReturnOnlyProdTags() {
        BundleTagFilterManager bundleTagFilterManager = new BundleTagFilterManager(List.of("prod", "came"));
        final List<String> actual = bundleTagFilterManager.filterTagsByEnvironment(allTags).collect(Collectors.toList());
        assertThat(actual).containsExactlyInAnyOrder(prodTags.toArray(new String[0]));
    }

    @Test
    void shouldReturnOnlyDevTags() {
        BundleTagFilterManager bundleTagFilterManager = new BundleTagFilterManager(List.of("dev"));
        final List<String> actual = bundleTagFilterManager.filterTagsByEnvironment(allTags).collect(Collectors.toList());
        assertThat(actual).containsExactlyInAnyOrder(devTags.toArray(new String[0]));
    }

    @Test
    void shouldReturnBothDevAndProdTags() {
        BundleTagFilterManager bundleTagFilterManager = new BundleTagFilterManager(List.of("dev", "prod"));
        final List<String> actual = bundleTagFilterManager.filterTagsByEnvironment(allTags).collect(Collectors.toList());
        assertThat(actual).containsExactlyInAnyOrder(prodAndDevTags.toArray(new String[0]));
    }

    @Test
    void shouldIgnoreNonKnownTagType() {
        BundleTagFilterManager bundleTagFilterManager = new BundleTagFilterManager(List.of("dev", "prod", "unknown"));
        final List<String> actual = bundleTagFilterManager.filterTagsByEnvironment(allTags).collect(Collectors.toList());
        assertThat(actual).containsExactlyInAnyOrder(prodAndDevTags.toArray(new String[0]));
    }
}
