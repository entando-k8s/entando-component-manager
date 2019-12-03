package org.entando.kubernetes.exception.digitalexchange;

import org.entando.kubernetes.exception.EntandoComponentManagerException;
import org.entando.kubernetes.exception.http.HttpNotFoundException;
import org.entando.web.exception.NotFoundException;

public class DigitalExchangeNotFoundException extends EntandoComponentManagerException implements
        HttpNotFoundException {

    public DigitalExchangeNotFoundException() {
        super("org.entando.digitalExchange.notFound");
    }
}
