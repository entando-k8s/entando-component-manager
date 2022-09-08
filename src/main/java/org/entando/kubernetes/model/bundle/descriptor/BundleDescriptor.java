package org.entando.kubernetes.model.bundle.descriptor;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRawValue;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.entando.kubernetes.model.bundle.BundleType;
import org.entando.kubernetes.model.bundle.descriptor.widget.KeepAsJsonDeserializer;
import org.springframework.util.ObjectUtils;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@Accessors(chain = true)
public class BundleDescriptor extends VersionedDescriptor {

    private String code;
    private String name;
    private String description;
    @JsonProperty("bundle-type")
    private BundleType bundleType;
    private ComponentSpecDescriptor components;
    @JsonDeserialize(using = KeepAsJsonDeserializer.class)
    @JsonRawValue
    private String ext;
    private String thumbnail;
    
    @Override
    public ComponentKey getComponentKey() {
        return ObjectUtils.isEmpty(code)
                ? new ComponentKey(name) :
                new ComponentKey(code);
    }
}
