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

    @GetMapping(value = {"/bundlegroups/{id}", "/bundlegroups/{id}/"}, produces = MediaType.APPLICATION_JSON_VALUE)
    PagedContent<BundleGroupVersionFilteredResponseView, BundleGroupVersionEntityDto> getBundleGroupVersionsAndFilterThem(
            @PathVariable(name = "id") String hubRegistryId, @RequestParam(name = "page") Integer page, @RequestParam(name = "pageSize") Integer pageSize,
            @RequestParam(required = false, name = "descriptorVersions") String[] descriptorVersions);

    @GetMapping(value = {"/bundles/{id}", "/bundles/{id}/"}, produces = MediaType.APPLICATION_JSON_VALUE)
    PagedContent<BundleDto, BundleEntityDto> getBundles(@PathVariable(name = "id") String hubRegistryId, @RequestParam(name = "page") Integer page,
            @RequestParam(name = "pageSize") Integer pageSize, @RequestParam(required = false, name = "bundleGroupId") String bundleGroupId,
            @RequestParam(required = false, name = "descriptorVersions") String[] descriptorVersions);
}
