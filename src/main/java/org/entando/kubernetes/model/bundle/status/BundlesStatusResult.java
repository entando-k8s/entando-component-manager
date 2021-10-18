package org.entando.kubernetes.model.bundle.status;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
@JsonInclude
public class BundlesStatusResult {

    private List<BundlesStatusItem> bundlesStatuses = new ArrayList<>();
}
