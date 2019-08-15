package org.entando.kubernetes.service.digitalexchange.signature;

public class SignatureMatchingException extends RuntimeException {
    public SignatureMatchingException(String cause) {
        super(cause);
    }
}
