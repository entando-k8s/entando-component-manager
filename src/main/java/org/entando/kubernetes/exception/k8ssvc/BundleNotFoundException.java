package org.entando.kubernetes.exception.k8ssvc;

import org.entando.kubernetes.exception.EntandoComponentManagerException;
import org.entando.kubernetes.exception.http.HttpNotFoundException;
import org.entando.kubernetes.exception.http.WithArgumentException;
import org.entando.kubernetes.exception.http.WithPredefinedMessage;

public class BundleNotFoundException extends EntandoComponentManagerException implements
        HttpNotFoundException, WithPredefinedMessage, WithArgumentException {

    private final transient Object[] args;

    public BundleNotFoundException(String bundleId) {
        super("bundle not found with bundleId: " + bundleId);
        this.args = new Object[]{bundleId};
    }

    @Override
    public Object[] getArgs() {
        return args;
    }

    @Override
    public String getPredefinedMessage() {
        return "org.entando.error.bundleNotFound";
    }
}
