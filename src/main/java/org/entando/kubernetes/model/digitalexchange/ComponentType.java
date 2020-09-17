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
    GROUP("group", 2),
    LABEL("label", 3),
    ASSET("asset", 4),
    WIDGET("widget", 5),
    CONTENT_TYPE("contentType", 6),
    CONTENT_TEMPLATE("contentTemplate", 7),
    FRAGMENT("fragment", 8),
    PAGE_TEMPLATE("pageTemplate", 9),
    PAGE("page", 10);


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
