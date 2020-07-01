package org.entando.kubernetes.model.digitalexchange;

import java.util.Arrays;

/**
 * The type of the component to be (or already) registered.
 *
 * @author Sergio Marcelino
 */
public enum ComponentType {

    PLUGIN("plugin", 0),
    DIRECTORY("directory", 1),
    LABEL("label",2),
    ASSET("asset", 3),
    WIDGET("widget", 4),
    CONTENT_TYPE("contentType", 5),
    CONTENT_TEMPLATE("contentTemplate", 6),
    FRAGMENT("fragment", 7),
    PAGE_TEMPLATE("pageTemplate", 8),
    PAGE("page", 9);

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
        return Arrays.stream(values()).anyMatch(e -> e.toString().equalsIgnoreCase(type));
    }

}
