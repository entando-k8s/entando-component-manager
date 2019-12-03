package org.entando.kubernetes.exception.k8ssvc;

import org.entando.kubernetes.exception.EntandoComponentManagerException;
import org.entando.kubernetes.exception.http.HttpNotFoundException;

public class DeploymentNotFoundException extends EntandoComponentManagerException implements HttpNotFoundException {

    public DeploymentNotFoundException() {
        super("org.entando.error.deploymentNotFound");
    }

}
