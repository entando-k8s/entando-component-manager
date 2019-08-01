package org.entando.kubernetes.service.digitalexchange.job.processors;

import org.entando.kubernetes.service.digitalexchange.job.ZipReader;
import org.entando.kubernetes.service.digitalexchange.model.DigitalExchange;

import java.io.IOException;

public interface ComponentProcessor<T> {

    void processComponent(DigitalExchange digitalExchange, String componentId,
                          T descriptor, ZipReader zipReader) throws IOException;

}
