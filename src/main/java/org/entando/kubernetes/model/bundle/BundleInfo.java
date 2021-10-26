package org.entando.kubernetes.model.bundle;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
public class BundleInfo {

    private String name;
    private String description;
    private String gitRepoAddress;
    private String descriptionImage;
    private List<String> bundleGroups;
    private String bundleId;
    private List<String> dependencies;
}
