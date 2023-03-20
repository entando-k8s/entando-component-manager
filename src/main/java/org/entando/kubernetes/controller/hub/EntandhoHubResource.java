package org.entando.kubernetes.controller.hub;

import org.entando.kubernetes.client.hub.domain.*;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@RequestMapping(value = "/hub")
public interface EntandhoHubResource {

    @GetMapping(value = "/bundlegroups", produces = {"application/json"})
    public PagedContent<BundleGroupVersionFilteredResponseView, BundleGroupVersionEntityDto> getaBundleGroupVersionsAndFilterThem(@RequestParam String host, @RequestParam Integer page, @RequestParam Integer pageSize, @RequestParam(required = false) String[] descriptorVersions);

    @GetMapping(value = "/bundles", produces = {"application/json"})
    PagedContent<BundleDto, BundleEntityDto> getBundles(@RequestParam String host, @RequestParam Integer page, @RequestParam Integer pageSize, @RequestParam(required = false) String bundleGroupId, @RequestParam(required = false) String[] descriptorVersions);
}
