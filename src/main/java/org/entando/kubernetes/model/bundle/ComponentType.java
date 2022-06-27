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
    CATEGORY("category",2),
    GROUP("group", 3),
    LANGUAGE("language", 4),
    LABEL("label", 5),
    RESOURCE("asset", 6),
    WIDGET("widget", 7),
    FRAGMENT("fragment", 8),
    CONTENT_TYPE("contentType", 9),
    CONTENT_TEMPLATE("contentTemplate", 10),
    ASSET("asset", 11),
    PAGE_TEMPLATE("pageTemplate", 12),
    PAGE("page", 13),
    CONTENT("content", 14),
    PAGE_CONFIGURATION("pageConfiguration", 15),

    // ALIASES
    WIDGET_CONFIG("widget-config", 7);

    public boolean isNotAlias() {
        return this != ComponentType.WIDGET_CONFIG;
    }

    private final String typeName;
    private final int installPriority;

    ComponentType(String typeName, int installPriority) {
        this.typeName = typeName;
        this.installPriority = installPriority;
    }

    public static boolean isValidType(String type) {
        return Arrays.stream(values()).anyMatch(e -> e.getTypeName().equalsIgnoreCase(type));
    }

    public String getTypeName() {
        return this.typeName;
    }

    public int getInstallPriority() {
        return installPriority;
    }
}
