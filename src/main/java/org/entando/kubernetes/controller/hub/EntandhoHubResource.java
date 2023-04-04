package org.entando.kubernetes.controller.hub;

import org.entando.kubernetes.client.hub.domain.BundleDto;
import org.entando.kubernetes.client.hub.domain.BundleEntityDto;
import org.entando.kubernetes.client.hub.domain.BundleGroupVersionEntityDto;
import org.entando.kubernetes.client.hub.domain.BundleGroupVersionFilteredResponseView;
import org.entando.kubernetes.client.hub.domain.PagedContent;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@RequestMapping(value = "/hub")
public interface EntandhoHubResource {

    @GetMapping(value = "/bundlegroups/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    PagedContent<BundleGroupVersionFilteredResponseView, BundleGroupVersionEntityDto> getBundleGroupVersionsAndFilterThem(
            @PathVariable(name = "id") String hubRegistryId, @RequestParam Integer page, @RequestParam Integer pageSize,
            @RequestParam(required = false) String[] descriptorVersions);

    @GetMapping(value = "/bundles/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    PagedContent<BundleDto, BundleEntityDto> getBundles(@PathVariable(name = "id") String hubRegistryId, @RequestParam Integer page,
            @RequestParam Integer pageSize, @RequestParam(required = false) String bundleGroupId,
            @RequestParam(required = false) String[] descriptorVersions);
}
