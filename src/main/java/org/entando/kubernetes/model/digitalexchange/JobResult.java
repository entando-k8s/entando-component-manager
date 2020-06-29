package org.entando.kubernetes.model.digitalexchange;

import java.util.Optional;
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

    public Optional<Exception> getException() {
        return Optional.ofNullable(this.exception);
    }

    public boolean hasException() {
        return this.exception != null;
    }

}
