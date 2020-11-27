package org.entando.kubernetes.exception.digitalexchange;

import org.entando.kubernetes.exception.EntandoComponentManagerException;

public class BundleOperationConcurrencyException extends EntandoComponentManagerException {

    public BundleOperationConcurrencyException(String message) {
        super(message);
    }
}
