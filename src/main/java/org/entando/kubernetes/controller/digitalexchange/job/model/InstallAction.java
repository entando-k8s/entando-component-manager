package org.entando.kubernetes.controller.digitalexchange.job.model;

public enum InstallAction {

    CREATE,  //Always try to create the object
    SKIP,    //Skip object if conflict
    OVERRIDE //Update object if conflict
}
