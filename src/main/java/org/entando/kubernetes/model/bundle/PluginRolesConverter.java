package org.entando.kubernetes.model.bundle;

import jakarta.persistence.AttributeConverter;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;

public class PluginRolesConverter implements AttributeConverter<Set<String>, String> {

    @Override
    public String convertToDatabaseColumn(Set<String> strings) {
        return StringUtils.join(strings, ",");
    }

    @Override
    public Set<String> convertToEntityAttribute(String s) {
        return Optional.ofNullable(StringUtils.split(s, ","))
                .map(split -> Arrays.stream(split).collect(Collectors.toSet())).orElse(null);
    }
}
