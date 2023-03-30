package org.entando.kubernetes.client.hub;

import java.util.Map;
import org.entando.kubernetes.client.hub.domain.BundleDto;
import org.entando.kubernetes.client.hub.domain.BundleEntityDto;
import org.entando.kubernetes.client.hub.domain.BundleGroupVersionEntityDto;
import org.entando.kubernetes.client.hub.domain.BundleGroupVersionFilteredResponseView;
import org.entando.kubernetes.client.hub.domain.PagedContent;

public interface HubClient {

    ProxiedPayload<PagedContent<BundleGroupVersionFilteredResponseView, BundleGroupVersionEntityDto>> searchBundleGroupVersions(
            String host, Map<String, Object> params);

    ProxiedPayload<PagedContent<BundleDto,BundleEntityDto>> getBundles(String host, Map<String, Object> params);
}
