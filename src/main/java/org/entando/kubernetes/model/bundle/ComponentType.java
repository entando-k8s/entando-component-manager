package org.entando.kubernetes.model.bundle;

import java.util.Arrays;

/**
 * The type of the component to be (or already) registered.
 *
 * @author Sergio Marcelino
 */
public enum ComponentType {

    PLUGIN("plugin"),
    DIRECTORY("directory"),
    CATEGORY("category"),
    GROUP("group"),
    LANGUAGE("language"),
    LABEL("label"),
    RESOURCE("asset"),
    WIDGET("widget"),
    CONTENT_TYPE("contentType"),
    CONTENT_TEMPLATE("contentTemplate"),
    ASSET("asset"),
    CONTENT("content"),
    FRAGMENT("fragment"),
    PAGE_TEMPLATE("pageTemplate"),
    PAGE("page");

    private final String typeName;

    ComponentType(String typeName) {
        this.typeName = typeName;
    }

    public static boolean isValidType(String type) {
        return Arrays.stream(values()).anyMatch(e -> e.getTypeName().equalsIgnoreCase(type));
    }

    public String getTypeName() {
        return this.typeName;
    }
}
