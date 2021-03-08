package org.entando.kubernetes.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum EntandoBundleJobErrors {

    GENERIC(100);

    private final int code;
}
