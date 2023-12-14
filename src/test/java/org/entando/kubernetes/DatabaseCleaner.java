package org.entando.kubernetes;

import org.entando.kubernetes.repository.EntandoBundleComponentJobRepository;
import org.entando.kubernetes.repository.EntandoBundleJobRepository;
import org.entando.kubernetes.repository.InstalledEntandoBundleRepository;
import org.entando.kubernetes.repository.PluginDataRepository;
import org.entando.kubernetes.utils.TenantTestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class DatabaseCleaner {

    @Autowired
    EntandoBundleJobRepository jobRepository;

    @Autowired
    EntandoBundleComponentJobRepository jobComponentRepository;

    @Autowired
    InstalledEntandoBundleRepository installedComponentRepository;

    @Autowired
    PluginDataRepository pluginDataRepository;

    public void cleanup() {

        TenantTestUtils.setPrimaryTenant();

        installedComponentRepository.deleteAll();
        jobComponentRepository.deleteAll();
        jobRepository.deleteAll();
        pluginDataRepository.deleteAll();
    }

}
