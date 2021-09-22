package org.entando.kubernetes.stubhelper;

import org.entando.kubernetes.model.job.EntandoBundleJobEntity;
import org.entando.kubernetes.model.job.JobStatus;

public class EntandoBundleJobStubHelper {

    public static final String COMPONENT_ID = "comp-id";
    public static final String COMPONENT_NAME = "comp-name";
    public static final String COMPONENT_VERSION = "v1.0.0";

    public static EntandoBundleJobEntity stubEntandoBundleJobEntity(JobStatus jobStatus) {
        EntandoBundleJobEntity entity = new EntandoBundleJobEntity();
        entity.setComponentId(COMPONENT_ID);
        entity.setComponentName(COMPONENT_NAME);
        entity.setComponentVersion(COMPONENT_VERSION);
        entity.setStatus(jobStatus);
        return entity;
    }
}
