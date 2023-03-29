package org.entando.kubernetes.exception.registry;

import org.entando.kubernetes.exception.EntandoComponentManagerException;
import org.entando.kubernetes.exception.http.HttpNotFoundException;

public class RegistryNotFoundException extends EntandoComponentManagerException implements HttpNotFoundException {

    public RegistryNotFoundException(String message) {
        super(message);
    }
}
