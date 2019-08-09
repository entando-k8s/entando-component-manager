package org.entando.kubernetes.exception;

import org.entando.web.exception.NotFoundException;

public class JobNotFoundException extends NotFoundException {

    public JobNotFoundException() {
        super("org.entando.error.jobNotFound");
    }

}
