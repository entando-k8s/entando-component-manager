package org.entando.kubernetes.client.hub;

import java.util.Map;
import org.entando.kubernetes.client.hub.domain.BundleDto;
import org.entando.kubernetes.client.hub.domain.BundleEntityDto;
import org.entando.kubernetes.client.hub.domain.BundleGroupVersionEntityDto;
import org.entando.kubernetes.client.hub.domain.BundleGroupVersionFilteredResponseView;
import org.entando.kubernetes.client.hub.domain.PagedContent;
import org.entando.kubernetes.model.entandohub.EntandoHubRegistry;

public interface HubClient {

    ProxiedPayload<PagedContent<BundleGroupVersionFilteredResponseView, BundleGroupVersionEntityDto>> searchBundleGroupVersions(
            EntandoHubRegistry registry, Map<String, Object> params);

    ProxiedPayload<PagedContent<BundleDto, BundleEntityDto>> getBundles(EntandoHubRegistry registry, Map<String, Object> params);
}
