package org.entando.kubernetes.exception.job;

import org.entando.kubernetes.exception.EntandoComponentManagerException;
import org.entando.kubernetes.exception.http.HttpNotFoundException;
import org.entando.kubernetes.exception.http.WithArgumentException;
import org.entando.kubernetes.exception.http.WithPredefinedMessage;

public class JobNotFoundException extends EntandoComponentManagerException
        implements HttpNotFoundException, WithPredefinedMessage, WithArgumentException {

    private final transient Object[] args;

    public JobNotFoundException(String componentId) {
        super();
        this.args = new Object[]{componentId};
    }

    @Override
    public Object[] getArgs() {
        return args;
    }

    @Override
    public String getPredefinedMessage() {
        return "org.entando.error.jobNotFound";
    }
}
