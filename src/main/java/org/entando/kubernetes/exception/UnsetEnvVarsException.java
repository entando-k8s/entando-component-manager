package org.entando.kubernetes.exception;

import org.entando.kubernetes.exception.http.HttpBadRequestException;
import org.entando.kubernetes.exception.http.WithArgumentException;
import org.entando.kubernetes.exception.http.WithPredefinedMessage;

public class UnsetEnvVarsException extends EntandoComponentManagerException implements
        HttpBadRequestException,
        WithPredefinedMessage,
        WithArgumentException {

    private transient final Object[] envs;

    public UnsetEnvVarsException(final Object ... envs) {
        super();
        this.envs = envs;
    }

    @Override
    public Object[] getArgs() {
        return envs;
    }

    @Override
    public String getPredefinedMessage() {
        return "org.entando.error.unsetVarsException";
    }
}
