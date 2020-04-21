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
    WIDGET,

    /**
     * A Page Model
     */
    PAGE_MODEL,

    /**
     * A Page
     */
    PAGE,

    /**
     * A Content Model from CMS
     */
    CONTENT_MODEL,

    /**
     * A Content Type from CMS
     */
    CONTENT_TYPE,

    /**
     * A label
     */
    LABEL,

    /**
     * A static resource (AKA asset)
     */
    RESOURCE,

    /**
     * A Service Deployment on Kubernetes (or any similar platform).
     */
    PLUGIN,

    /**
     * A Gui fragment
     */
    FRAGMENT;

    public static boolean isValidType(String type) {
        return Arrays.stream(values()).anyMatch(e -> e.toString().equalsIgnoreCase(type));
    }
}
