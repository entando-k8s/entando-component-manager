package org.entando.kubernetes.exception.digitalexchange;

import org.entando.kubernetes.exception.EntandoComponentManagerException;
import org.entando.kubernetes.exception.http.HttpNotFoundException;

public class BundleNotInstalledException extends EntandoComponentManagerException implements HttpNotFoundException {

    public BundleNotInstalledException(String message) {
        super(message);
    }
}
