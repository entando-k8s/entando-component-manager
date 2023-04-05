package org.entando.kubernetes.client;

import java.util.Map;
import lombok.Setter;
import org.entando.kubernetes.client.hub.HubClient;
import org.entando.kubernetes.client.hub.ProxiedPayload;
import org.entando.kubernetes.client.hub.domain.BundleDto;
import org.entando.kubernetes.client.hub.domain.BundleEntityDto;
import org.entando.kubernetes.client.hub.domain.BundleGroupVersionEntityDto;
import org.entando.kubernetes.client.hub.domain.BundleGroupVersionFilteredResponseView;
import org.entando.kubernetes.client.hub.domain.PagedContent;
import org.entando.kubernetes.exception.web.InternalServerException;
import org.entando.kubernetes.model.entandohub.EntandoHubRegistry;
import org.entando.kubernetes.stubhelper.HubStubHelper;
import org.springframework.http.HttpStatus;

@Setter
public class HubClientTestDouble implements HubClient {

    private boolean mustFail;

    @Override
    public ProxiedPayload<PagedContent<BundleGroupVersionFilteredResponseView, BundleGroupVersionEntityDto>> searchBundleGroupVersions(
            EntandoHubRegistry registry, Map<String, Object> params) {

        if (mustFail) {
            return asBundleGroupVersionsDto(createWithError());
        }

        return HubStubHelper.stubBundleGroupVersionsProxiedPayload();
    }

    @Override
    public ProxiedPayload<PagedContent<BundleDto, BundleEntityDto>> getBundles(EntandoHubRegistry registry,
            Map<String, Object> params) {

        if (mustFail) {
            return asBundleDto(createWithError());
        }

        return HubStubHelper.stubBundleDtosProxiedPayload();
    }

    private ProxiedPayload<?> createWithError() {
        return ProxiedPayload.builder()
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .exceptionClass(InternalServerException.class.getSimpleName())
                .exceptionMessage("Generic error")
                .build();
    }

    private ProxiedPayload<PagedContent<BundleGroupVersionFilteredResponseView, BundleGroupVersionEntityDto>> asBundleGroupVersionsDto(
            ProxiedPayload<?> proxiedPayload) {
        return (ProxiedPayload<PagedContent<BundleGroupVersionFilteredResponseView, BundleGroupVersionEntityDto>>) proxiedPayload;
    }

    private ProxiedPayload<PagedContent<BundleDto, BundleEntityDto>> asBundleDto(
            ProxiedPayload<?> proxiedPayload) {
        return (ProxiedPayload<PagedContent<BundleDto, BundleEntityDto>>) proxiedPayload;
    }
}
