package org.entando.kubernetes.model.bundle.descriptor;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PageDescriptor implements Descriptor {

    public static final String STUB_SUFFIX = " - STUB";

    private String code;
    private String parentCode;
    private String ownerGroup;
    private String pageModel;
    @Setter(AccessLevel.NONE)
    private Map<String, String> titles;

    public PageDescriptor setTitles(Map<String, String> titles) {
        this.titles = titles.entrySet().stream()
                .map(entry -> {
                    entry.setValue(entry.getValue() + STUB_SUFFIX);
                    return entry;
                })
                .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
        return this;
    }

    @Override
    public ComponentKey getComponentKey() {
        return new ComponentKey(code);
    }
}
