package org.entando.kubernetes.exception.k8ssvc;

import org.entando.kubernetes.exception.EntandoComponentManagerException;
import org.entando.kubernetes.exception.http.HttpNotFoundException;
import org.entando.kubernetes.exception.http.WithPredefinedMessage;

public class DeploymentNotFoundException extends EntandoComponentManagerException
        implements HttpNotFoundException, WithPredefinedMessage {

    @Override
    public String getPredefinedMessage() {
        return "org.entando.error.deploymentNotFound";
    }
}
