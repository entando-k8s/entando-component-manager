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
    LABEL("label", 4),
    ASSET("asset", 5),
    WIDGET("widget", 6),
    CONTENT_TYPE("contentType", 7),
    CONTENT_TEMPLATE("contentTemplate", 8),
    FRAGMENT("fragment", 9),
    PAGE_TEMPLATE("pageTemplate", 10),
    PAGE("page", 11);


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
