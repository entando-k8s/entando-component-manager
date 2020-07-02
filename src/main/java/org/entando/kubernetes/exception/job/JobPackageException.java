package org.entando.kubernetes.exception.job;

import org.entando.kubernetes.exception.EntandoComponentManagerException;

import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;

public class JobPackageException extends EntandoComponentManagerException {

    private final AtomicReference<Path> packagePath = new AtomicReference<>();

    public JobPackageException(Path packagePath) {
        super();
        this.packagePath.set(packagePath);
    }

    public JobPackageException(Path packagePath, String message) {
        super(message);
        this.packagePath.set(packagePath);
    }

    public JobPackageException(Path packagePath, String message, Throwable cause) {
        super(message, cause);
        this.packagePath.set(packagePath);
    }

    public JobPackageException(Path packagePath, Throwable cause) {
        super(cause);
        this.packagePath.set(packagePath);
    }

    public Path getPackagePath() {
        return packagePath.get();
    }
}
