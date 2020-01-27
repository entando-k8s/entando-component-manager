package org.entando.kubernetes.exception.digitalexchange;

import org.entando.kubernetes.exception.EntandoComponentManagerException;
import org.entando.kubernetes.exception.http.HttpNotFoundException;
import org.entando.kubernetes.exception.http.WithPredefinedMessage;

public class DigitalExchangeNotFoundException extends EntandoComponentManagerException
        implements HttpNotFoundException, WithPredefinedMessage {

    @Override
    public String getPredefinedMessage() {
        return "org.entando.digitalExchange.notFound";
    }
}
