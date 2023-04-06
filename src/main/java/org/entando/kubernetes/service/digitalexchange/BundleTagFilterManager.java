package org.entando.kubernetes.service.digitalexchange;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class BundleTagFilterManager {

    // The filter to check for semantic versioning is made elsewhere, here we want only to split tags by their type (environment)

    private static final Pattern prodBundleTagPattern = Pattern.compile("^v?(\\d*)\\.(\\d*)\\.(\\d*)(-(fix\\.\\d*|patch\\.\\d*))?$");
    private static final Pattern devBundleTagPattern = Pattern.compile("^v?(\\d*)\\.(\\d*)\\.(\\d*)(?!(-(fix\\.\\d*|patch\\.\\d*)))(-.+)$");

    private static final String BUNDLE_TYPE_PRODUCTION = "prod";
    private static final String BUNDLE_TYPE_DEVELOPMENT = "dev";
    private final Map<String, Boolean> allowedBundleTagTypes = Map.of(
            BUNDLE_TYPE_PRODUCTION, true,
            BUNDLE_TYPE_DEVELOPMENT, true);

    private final List<String> bundleTagTypes;

    private final Map<String, Predicate<String>> strategies = Map.of(
            BUNDLE_TYPE_PRODUCTION, tag -> prodBundleTagPattern.matcher(tag).matches(),
            BUNDLE_TYPE_DEVELOPMENT, tag -> devBundleTagPattern.matcher(tag).matches());

    public BundleTagFilterManager(@Value("${entando.bundle.tags.types:prod}") List<String> bundleTagTypes) {
        this.bundleTagTypes = bundleTagTypes;

        this.bundleTagTypes.forEach(tagType -> {
            if (! allowedBundleTagTypes.containsKey(tagType)) {
                log.info("Unsupported bundle tag type \"{}\" found in configuration. Supported tag types are {}",
                        tagType, String.join(", ", allowedBundleTagTypes.keySet()));
            }
        });

        log.info("Bundle tag types loaded: \"{}\"", String.join(",", this.bundleTagTypes));
    }


    public Stream<String> filterTagsByEnvironment(List<String> tags) {

        return bundleTagTypes.stream()
                .flatMap(tagType -> {
                    final Predicate<String> strategy = strategies.getOrDefault(tagType, s -> false);
                    return tags.stream().filter(strategy);
                })
                .distinct();
    }
}
