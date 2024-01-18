package org.entando.kubernetes.model.bundle.processor;

import java.util.function.Consumer;
import java.util.function.Supplier;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;

@UtilityClass
public class ProcessorHelper {

    public static final String BUNDLE_ID_PLACEHOLDER = "__BUNDLE_ID__";

    /**
     * replace any occurrence of the bundle id placeholder in the received input string.
     * @param bundleId the bundle id to use as replacement value
     * @param getter the supplier providing the input in which search the placeholder
     * @param setter the consumer to set the new replaced value
     */
    public static void applyBundleIdPlaceholderReplacement(String bundleId, Supplier<String> getter, Consumer<String> setter) {
        final String replacedValue = replaceBundleIdPlaceholder(getter.get(), bundleId);
        setter.accept(replacedValue);
    }

    /**
     * replace any occurrence of the bundle id placeholder in the received input string.
     * @param input the string in which apply the replacement
     * @param bundleId the string to use as replacement value
     * @return the string containing the replaced value
     */
    public static String replaceBundleIdPlaceholder(String input, String bundleId) {
        if (StringUtils.isEmpty(input)) {
            return input;
        }
        return input.replace(BUNDLE_ID_PLACEHOLDER, bundleId);
    }
}
