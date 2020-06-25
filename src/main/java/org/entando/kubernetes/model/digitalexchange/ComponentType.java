package org.entando.kubernetes.model.digitalexchange;

import java.util.Arrays;

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
     * A static asset
     */
    ASSET("asset"),

    /**
     * A static asset directory
     */
    DIRECTORY("directory"),

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

    public static boolean isValidType(String type) {
        return Arrays.stream(values()).anyMatch(e -> e.toString().equalsIgnoreCase(type));
    }
}
