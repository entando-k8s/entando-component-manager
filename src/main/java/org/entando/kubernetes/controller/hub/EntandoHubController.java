package org.entando.kubernetes.controller.hub;

import java.util.HashMap;
import java.util.Map;
import org.entando.kubernetes.client.hub.ProxiedPayload;
import org.entando.kubernetes.client.hub.domain.BundleDto;
import org.entando.kubernetes.client.hub.domain.BundleEntityDto;
import org.entando.kubernetes.client.hub.domain.BundleGroupVersionEntityDto;
import org.entando.kubernetes.client.hub.domain.BundleGroupVersionFilteredResponseView;
import org.entando.kubernetes.client.hub.domain.PagedContent;
import org.entando.kubernetes.service.HubService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;


@RestController
public class EntandoHubController implements EntandhoHubResource {

    private final HubService hubService;
    Logger log = LoggerFactory.getLogger(EntandoHubController.class);

    public EntandoHubController(HubService hubService) {
        this.hubService = hubService;
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

    @Override
    public PagedContent<BundleGroupVersionFilteredResponseView, BundleGroupVersionEntityDto> getBundleGroupVersionsAndFilterThem(
            @PathVariable String id,
            @RequestParam Integer page,
            @RequestParam Integer pageSize,
            @RequestParam(required = false) String[] descriptorVersions) {
        try {
            Map<String, Object> params = getParamsToMap(page, pageSize, descriptorVersions);

            ProxiedPayload<PagedContent<BundleGroupVersionFilteredResponseView, BundleGroupVersionEntityDto>> clientResponse
                    = hubService.searchBundleGroupVersions(id, params);

            // TODO should this return the status code gotten by the hub client?
            if (clientResponse.hasError()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY, "STATUS: " + clientResponse.getStatus() + "\nEXCEPTION MESSAGE: "
                        + clientResponse.getExceptionMessage());
            }

            return clientResponse.getPayload();
        } catch (RuntimeException t) {
            log.error("error getting bundle groups!", t);
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR, "error in getBundleGroupVersionsAndFilterThem ", t);
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

            ProxiedPayload<PagedContent<BundleDto, BundleEntityDto>> clientResponse = hubService.getBundles(id, params);

            // TODO should this return the status code got by the hub client?
            if (clientResponse.hasError()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY, "STATUS: " + clientResponse.getStatus() + "\nEXCEPTION MESSAGE: "
                        + clientResponse.getExceptionMessage());
            }
            return clientResponse.getPayload();
        } catch (RuntimeException t) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR, "error in getBundles ", t);
        }
    }

}
