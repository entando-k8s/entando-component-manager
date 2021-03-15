package org.entando.kubernetes.controller.digitalexchange.job.model;

import java.time.LocalDateTime;
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
public class AnalysisReportComponentResult {

    private Status status;
    private LocalDateTime updateTime;
    private String hash;
}
