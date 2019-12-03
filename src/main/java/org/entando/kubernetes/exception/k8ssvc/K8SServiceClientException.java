package org.entando.kubernetes.exception.k8ssvc;

import org.entando.kubernetes.exception.EntandoComponentManagerException;

public class K8SServiceClientException extends EntandoComponentManagerException {

    public K8SServiceClientException() {
        super();
    }

    public K8SServiceClientException(String message) {
        super(message);
    }

    public K8SServiceClientException(String message, Throwable cause) {
        super(message, cause);
    }

    public K8SServiceClientException(Throwable cause) {
        super(cause);
    }

    protected K8SServiceClientException(String message, Throwable cause, boolean enableSuppression,
            boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
