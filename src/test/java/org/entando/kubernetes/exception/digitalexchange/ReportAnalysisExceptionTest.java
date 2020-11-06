package org.entando.kubernetes.exception.digitalexchange;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class ReportAnalysisExceptionTest {

    @Test
    void shouldContainTheRightMessage() {
        String mex = "error message";
        ReportAnalysisException reportAnalysisException = new ReportAnalysisException(mex);
        assertThat(reportAnalysisException.getMessage()).isEqualTo(mex);
    }
}