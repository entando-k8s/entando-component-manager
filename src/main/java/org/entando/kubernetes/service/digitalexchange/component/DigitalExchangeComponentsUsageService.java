package org.entando.kubernetes.service.digitalexchange.component;

import org.entando.kubernetes.client.core.EntandoCoreClient;
import org.springframework.stereotype.Service;

@Service
public class DigitalExchangeComponentsUsageService {

    private final EntandoCoreClient coreService;

    public DigitalExchangeComponentsUsageService(EntandoCoreClient coreService) {
        this.coreService = coreService;
    }

}
