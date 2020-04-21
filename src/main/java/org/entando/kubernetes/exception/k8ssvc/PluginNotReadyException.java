package org.entando.kubernetes.exception.k8ssvc;

import org.entando.kubernetes.exception.EntandoComponentManagerException;
import org.entando.kubernetes.exception.http.HttpException;
import org.springframework.http.HttpStatus;

public class PluginNotReadyException extends EntandoComponentManagerException
        implements HttpException {

    public PluginNotReadyException(String pluginId) {
        super("Plugin " + pluginId + " not ready after predefined timeout");
    }

    @Override
    public HttpStatus getStatus() {
        return HttpStatus.REQUEST_TIMEOUT;
    }
}

