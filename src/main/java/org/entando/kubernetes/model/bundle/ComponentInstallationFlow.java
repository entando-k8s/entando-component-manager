package org.entando.kubernetes.model.bundle;

/**
 * Represents the flow of the installation process.
 * Since it differs from the component list, we are using a different enum instead of heaping all in the ComponentType
 */
public enum ComponentInstallationFlow {

    PLUGIN(0),
    DIRECTORY(1),
    CATEGORY(2),
    GROUP(3),
    LANGUAGE(4),
    LABEL(5),
    RESOURCE(6),
    WIDGET(7),
    CONTENT_TYPE(8),
    CONTENT_TEMPLATE(9),
    ASSET(10),
    PAGE_TEMPLATE(11),
    PAGE_INIT(12),
    CONTENT(13),
    FRAGMENT(14),
    PAGE_POPULATION(15);

    private final int installPriority;

    ComponentInstallationFlow(int installPriority) {
        this.installPriority = installPriority;
    }

    public int getInstallPriority() {
        return this.installPriority;
    }

}
