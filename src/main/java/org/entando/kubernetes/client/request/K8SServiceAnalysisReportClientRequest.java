package org.entando.kubernetes.client.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Value;

@Value
@AllArgsConstructor
@JsonInclude(Include.NON_NULL)
public class K8SServiceAnalysisReportClientRequest implements AnalysisReportClientRequest {

    List<String> plugins;
}
