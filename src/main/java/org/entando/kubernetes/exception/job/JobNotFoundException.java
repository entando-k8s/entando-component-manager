package org.entando.kubernetes.exception.job;

import org.entando.kubernetes.exception.EntandoComponentManagerException;
import org.entando.kubernetes.exception.http.HttpNotFoundException;

public class JobNotFoundException extends EntandoComponentManagerException implements HttpNotFoundException {

    public JobNotFoundException() {
        super("org.entando.error.jobNotFound");
    }

}
