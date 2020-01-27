package org.entando.kubernetes.exception.k8ssvc;

import org.entando.kubernetes.exception.EntandoComponentManagerException;
import org.entando.kubernetes.exception.http.HttpBadRequestException;
import org.entando.kubernetes.exception.http.WithPredefinedMessage;

public class PluginAlreadyDeployedException extends EntandoComponentManagerException
        implements HttpBadRequestException, WithPredefinedMessage {

    @Override
    public String getPredefinedMessage() {
        return "org.entando.error.pluginAlreadyDeployed";
    }
}
