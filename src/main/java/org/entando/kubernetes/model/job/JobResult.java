package org.entando.kubernetes.model.job;

import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.entando.kubernetes.exception.EntandoComponentManagerException;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobResult {

    JobStatus status;
    EntandoComponentManagerException installException;
    EntandoComponentManagerException rollbackException;
    Double progress;

    public boolean hasException() {
        return this.installException != null || this.rollbackException != null;
    }

    public Integer getInstallErrorCode() {
        return null != installException ? installException.getErrorCode() : null;
    }

    public String getInstallErrorMessage() {
        return null != installException ? installException.getMessage() : null;
    }

    public String getInstallError() {
        return this.formatError(installException);
    }

    public Integer getRollbackErrorCode() {
        return null != rollbackException ? rollbackException.getErrorCode() : null;
    }

    public String getRollbackErrorMessage() {
        return null != rollbackException ? rollbackException.getMessage() : null;
    }

    public String getRollbackError() {
        return this.formatError(rollbackException);
    }

    public JobResult setInstallException(Exception ex) {
        this.installException = ex instanceof EntandoComponentManagerException
                ? (EntandoComponentManagerException) ex
                : new EntandoComponentManagerException(ex);
        return this;
    }

    public JobResult setRollbackException(Exception ex) {
        this.rollbackException = ex instanceof EntandoComponentManagerException
                ? (EntandoComponentManagerException) ex
                : new EntandoComponentManagerException(ex);
        return this;
    }

    public void clearException() {
        this.installException = null;
        this.rollbackException = null;
    }

    private String formatError(EntandoComponentManagerException e) {
        return Optional.ofNullable(e)
                .map(ex -> String.format("code: %d --- message: %s", ex.getErrorCode(), ex.getMessage()))
                .orElse("");
    }
}
