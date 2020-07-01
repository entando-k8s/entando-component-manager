package org.entando.kubernetes.model.job;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobResult {

    JobStatus status;
    Exception exception;

    public Exception getException() {
        return exception;
    }

    public boolean hasException() {
        return this.exception != null;
    }

    public String getErrorMessage() {
        return exception.getMessage();
    }

    public void clearException() {
        this.exception = null;
    }
}
