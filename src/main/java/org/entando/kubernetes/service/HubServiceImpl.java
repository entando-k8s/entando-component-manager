package org.entando.kubernetes.service;

import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.entando.kubernetes.client.hub.HubClient;
import org.entando.kubernetes.client.hub.ProxiedPayload;
import org.entando.kubernetes.client.hub.domain.BundleDto;
import org.entando.kubernetes.client.hub.domain.BundleEntityDto;
import org.entando.kubernetes.client.hub.domain.BundleGroupVersionEntityDto;
import org.entando.kubernetes.client.hub.domain.BundleGroupVersionFilteredResponseView;
import org.entando.kubernetes.client.hub.domain.PagedContent;
import org.entando.kubernetes.model.entandohub.EntandoHubRegistry;
import org.entando.kubernetes.service.digitalexchange.entandohub.EntandoHubRegistryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class HubServiceImpl implements HubService {

    private final HubClient hubClientService;
    private final EntandoHubRegistryService registryService;

    @Autowired
    public HubServiceImpl(HubClient hubClientService, EntandoHubRegistryService registryService) {
        this.hubClientService = hubClientService;
        this.registryService = registryService;
    }

    @Override
    public ProxiedPayload<PagedContent<BundleGroupVersionFilteredResponseView, BundleGroupVersionEntityDto>> searchBundleGroupVersions(
            String hostId, Map<String, Object> params) {

        final EntandoHubRegistry registry = registryService.getRegistry(hostId);
        return hubClientService.searchBundleGroupVersions(registry, params);
    }

    @Override
    public ProxiedPayload<PagedContent<BundleDto, BundleEntityDto>> getBundles(String hostId,
            Map<String, Object> params) {

        final EntandoHubRegistry registry = registryService.getRegistry(hostId);
        return hubClientService.getBundles(registry, params);
    }
}
