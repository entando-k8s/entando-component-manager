package org.entando.kubernetes.model.bundle;

import jakarta.persistence.AttributeConverter;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public class BundleComponentTypesConverter implements AttributeConverter<Set<String>, String> {

    @Override
    public String convertToDatabaseColumn(Set<String> strings) {
        return String.join(", ", strings);
    }

    @Override
    public Set<String> convertToEntityAttribute(String s) {
        return Arrays.stream(s.split(", ")).map(String::trim).collect(Collectors.toSet());
    }
}
