package org.entando.kubernetes.exception.job;

import org.entando.kubernetes.exception.EntandoComponentManagerException;
import org.entando.kubernetes.exception.http.HttpNotFoundException;

public class UninstallJobNotFoundException extends EntandoComponentManagerException implements HttpNotFoundException {


    public UninstallJobNotFoundException(String message) {
        super(message);
    }

}
