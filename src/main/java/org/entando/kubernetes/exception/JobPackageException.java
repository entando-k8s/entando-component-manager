package org.entando.kubernetes.exception;

import java.nio.file.Path;

public class JobPackageException extends RuntimeException{

    private final Path packagePath;

    public JobPackageException(Path packagePath) {
        super();
        this.packagePath = packagePath;
    }

    public JobPackageException(Path packagePath, String message) {
        super(message);
        this.packagePath = packagePath;
    }

    public JobPackageException(Path packagePath, String message, Throwable cause) {
        super(message, cause);
        this.packagePath = packagePath;
    }

    public JobPackageException(Path packagePath, Throwable cause) {
        super(cause);
        this.packagePath = packagePath;
    }

    public Path getPackagePath() {
        return packagePath;
    }
}
