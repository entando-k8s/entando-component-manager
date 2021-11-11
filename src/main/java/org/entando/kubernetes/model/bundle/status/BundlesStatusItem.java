package org.entando.kubernetes.model.bundle.status;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.entando.kubernetes.model.bundle.BundleStatus;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
@JsonInclude
public class BundlesStatusItem {

    private String id;
    private String name;
    private BundleStatus status;
    private String installedVersion;
}
