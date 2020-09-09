package org.entando.kubernetes.model.job;

import org.entando.kubernetes.model.bundle.installable.Installable;

public interface HasInstallable {

    Installable getInstallable();

    void setInstallable(Installable installable);

}
