package org.entando.kubernetes.model.digitalexchange;

/**
 * The type of the component to be (or already) registered.
 *
 * @author Sergio Marcelino
 */
public enum ComponentType {

    /**
     * A Widget
     */
    WIDGET("widget"),

    /**
     * A Page template
     */
    PAGE_TEMPLATE("pageTemplate"),

    /**
     * A Page
     */
    PAGE("page"),

    /**
     * A Content template from CMS
     */
    CONTENT_TEMPLATE("contentTemplate"),

    /**
     * A Content Type from CMS
     */
    CONTENT_TYPE("contentType"),

    /**
     * A label
     */
    LABEL("label"),

    /**
     * A static resource (AKA asset)
     */
    RESOURCE("resource"),

    /**
     * A Service Deployment on Kubernetes (or any similar platform).
     */
    PLUGIN("plugin"),

    /**
     * A fragment
     */
    FRAGMENT("fragment");

    private String typeName;

    ComponentType(String typeName) {
        this.typeName = typeName;
    }

    public String getTypeName() {
        return this.typeName;
    }
}
