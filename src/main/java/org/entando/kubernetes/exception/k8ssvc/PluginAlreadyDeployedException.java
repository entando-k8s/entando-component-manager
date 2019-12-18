package org.entando.kubernetes.exception.k8ssvc;

import org.entando.kubernetes.exception.EntandoComponentManagerException;
import org.entando.kubernetes.exception.http.HttpBadRequestException;

public class PluginAlreadyDeployedException extends EntandoComponentManagerException implements HttpBadRequestException {

    public PluginAlreadyDeployedException() {
        super("org.entando.error.pluginAlreadyDeployed");
    }

}
