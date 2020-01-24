package org.entando.kubernetes.exception.job;

import org.entando.kubernetes.exception.EntandoComponentManagerException;
import org.entando.kubernetes.exception.http.HttpNotFoundException;
import org.entando.kubernetes.exception.http.WithPredefinedMessage;

public class JobNotFoundException extends EntandoComponentManagerException
        implements HttpNotFoundException, WithPredefinedMessage {

    @Override
    public String getPredefinedMessage() {
        return "org.entando.error.jobNotFound";
    }
}
