package org.entando.kubernetes.controller.hub;

import org.entando.kubernetes.client.hub.HubClientService;
import org.entando.kubernetes.client.hub.ProxiedPayload;
import org.entando.kubernetes.client.hub.domain.BundleDto;
import org.entando.kubernetes.client.hub.domain.BundleEntityDto;
import org.entando.kubernetes.client.hub.domain.BundleGroupVersionEntityDto;
import org.entando.kubernetes.client.hub.domain.BundleGroupVersionFilteredResponseView;
import org.entando.kubernetes.client.hub.domain.PagedContent;
import org.entando.kubernetes.service.digitalexchange.entandohub.EntandoHubRegistryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.Map;


@RestController
public class EntandoHubController implements EntandhoHubResource {

    Logger log = LoggerFactory.getLogger(EntandoHubController.class);

    private final HubClientService hubClientService;
    private final EntandoHubRegistryService registryService;

    public EntandoHubController(HubClientService hubClientService, EntandoHubRegistryService registryService) {
        this.hubClientService = hubClientService;
        this.registryService = registryService;
    }


    @Override
    public PagedContent<BundleGroupVersionFilteredResponseView, BundleGroupVersionEntityDto> getBundleGroupVersionsAndFilterThem(
//            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader,
            @PathVariable String id,
            @RequestParam Integer page,
            @RequestParam Integer pageSize,
            @RequestParam(required = false) String[] descriptorVersions) {
        try {
            Map<String, Object> params = getParamsToMap(page, pageSize, descriptorVersions);

            String host = registryService.getRegistry(id).getUrl();
            ProxiedPayload<PagedContent<BundleGroupVersionFilteredResponseView, BundleGroupVersionEntityDto>> clientResponse = hubClientService.searchBundleGroupVersions(host, params);
            if (clientResponse.hasError()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY, "STATUS: " + clientResponse.getStatus() + "\nEXCEPTION MESSAGE: " + clientResponse.getExceptionMessage());
            }
            // FIXME controllare che il risultato sia 200
            return (PagedContent<BundleGroupVersionFilteredResponseView, BundleGroupVersionEntityDto>) clientResponse.getPayload();
        } catch (Throwable t) {
            t.printStackTrace();
            log.error("error getting bundle groups!", t);
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR, "error in getaBundleGroupVersionsAndFilterThem ", t);
        }
    }

    @Override
    public PagedContent<BundleDto, BundleEntityDto> getBundles(@PathVariable String id,
                                                               @RequestParam Integer page,
                                                               @RequestParam Integer pageSize,
                                                               @RequestParam(required = false) String bundleGroupId,
                                                               @RequestParam(required = false) String[] descriptorVersions) {
        try {
            Map<String, Object> params = getParamsToMap(page, pageSize, descriptorVersions);
            params.put("bundleGroupId", bundleGroupId);

            String host = registryService.getRegistry(id).getUrl();
            ProxiedPayload<PagedContent<BundleDto, BundleEntityDto>> clientResponse = hubClientService.getBundles(host, params);

            if (clientResponse.hasError()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY, "STATUS: " + clientResponse.getStatus() + "\nEXCEPTION MESSAGE: " + clientResponse.getExceptionMessage());
            }
            return clientResponse.getPayload();
        } catch (Throwable t) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR, "error in getBundles ", t);
        }
    }

    private static Map<String, Object> getParamsToMap(Integer page, Integer pageSize, String[] descriptorVersions) {
        Map<String, Object> params = new HashMap<>();

        if (page != null) {
            params.put("page", page);
        }
        if (pageSize != null) {
            params.put("pageSize", pageSize);
        }
        if (descriptorVersions != null) {
            params.put("descriptorVersions", descriptorVersions);
        }
        return params;
    }

}
