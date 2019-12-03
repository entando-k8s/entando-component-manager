package org.entando.kubernetes.exception.k8ssvc;

import org.entando.kubernetes.exception.EntandoComponentManagerException;
import org.entando.kubernetes.exception.http.HttpNotFoundException;

public class PluginNotFoundException extends EntandoComponentManagerException implements HttpNotFoundException {

    public PluginNotFoundException() {
        super("org.entando.error.pluginNotFound");
    }

}
