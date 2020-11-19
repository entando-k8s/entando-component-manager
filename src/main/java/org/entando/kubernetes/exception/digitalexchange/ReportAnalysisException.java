package org.entando.kubernetes.exception.digitalexchange;

import org.entando.kubernetes.exception.EntandoComponentManagerException;
import org.entando.kubernetes.exception.http.HttpNotFoundException;

public class ReportAnalysisException extends EntandoComponentManagerException implements HttpNotFoundException {

    public ReportAnalysisException(String message) {
        super(message);
    }

    public ReportAnalysisException(String message, Throwable cause) {
        super(message, cause);
    }
}
