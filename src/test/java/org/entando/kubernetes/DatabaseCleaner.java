package org.entando.kubernetes;

import org.entando.kubernetes.repository.InstalledEntandoBundleRepository;
import org.entando.kubernetes.repository.EntandoBundleComponentJobRepository;
import org.entando.kubernetes.repository.EntandoBundleJobRepository;
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

    public void cleanup() {
        jobComponentRepository.deleteAll();
        jobRepository.deleteAll();
        installedComponentRepository.deleteAll();
    }

}
