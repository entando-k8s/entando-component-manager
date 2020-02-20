package org.entando.kubernetes.model.bundle;

public enum BundleProperty {

    DESCRIPTOR_FILENAME("descriptor.yaml"),
    RESOURCES_FOLDER_NAME("resources"),
    RESOURCES_FOLDER_PATH("resources/");


    private final String value;

    BundleProperty(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
