package org.entando.kubernetes.exception;

import org.entando.kubernetes.exception.http.HttpBadRequestException;
import org.entando.kubernetes.exception.http.WithArgumentException;

public class UnsetEnvVarsException extends EntandoComponentManagerException implements HttpBadRequestException, WithArgumentException {

    private final Object[] envs;

    public UnsetEnvVarsException(final Object... envs) {
        super("org.entando.error.unsetVarsException");
        this.envs = envs;
    }

    @Override
    public Object[] getArgs() {
        return envs;
    }
}
