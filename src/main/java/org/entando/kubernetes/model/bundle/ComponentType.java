package org.entando.kubernetes.model.bundle;

import java.util.Arrays;

/**
 * The type of the component to be (or already) registered.
 *
 * @author Sergio Marcelino
 */
public enum ComponentType {

    PLUGIN("plugin", 0),
    DIRECTORY("directory", 1),
    CATEGORY("category", 2),
    GROUP("group", 3),
    LANGUAGE("language", 4),
    LABEL("label", 5),
    RESOURCE("asset", 6),
    WIDGET("widget", 7),
    CONTENT_TYPE("contentType", 8),
    CONTENT_TEMPLATE("contentTemplate", 9),
    ASSET("asset", 10),
    CONTENT("content", 11),
    FRAGMENT("fragment", 12),
    PAGE_TEMPLATE("pageTemplate", 13),
    PAGE("page", 14);

    private String typeName;
    private int installPriority;

    ComponentType(String typeName, int installPriority) {
        this.typeName = typeName;
        this.installPriority = installPriority;
    }

    public String getTypeName() {
        return this.typeName;
    }

    public int getInstallPriority() {
        return this.installPriority;
    }

    public static boolean isValidType(String type) {
        return Arrays.stream(values()).anyMatch(e -> e.getTypeName().equalsIgnoreCase(type));
    }

}
