package org.entando.kubernetes.model.bundle;

import java.util.Arrays;
import lombok.extern.slf4j.Slf4j;

/**
 * The type of the component to be (or already) registered.
 *
 * @author Sergio Marcelino
 */

@Slf4j
public enum ComponentType {

    PLUGIN("plugin", "plugin", 0),
    DIRECTORY("directory", "directory", 1),
    CATEGORY("category", "category", 2),
    GROUP("group", "group", 3),
    LANGUAGE("language", "language", 4),
    LABEL("label", "label", 5),
    RESOURCE("asset", "asset", 6),
    WIDGET("widget", "widget", 7),
    FRAGMENT("fragment", "fragment", 8),
    CONTENT_TYPE("contentType", "contentType", 9),
    CONTENT_TEMPLATE("contentTemplate", "contentTemplate", 10),
    ASSET("asset", "asset", 11),
    PAGE_TEMPLATE("pageTemplate", "pageModel", 12),
    PAGE("page", "page", 13),
    CONTENT("content", "content", 14),
    PAGE_CONFIGURATION("pageConfiguration", "pageConfiguration", 15);

    private final String typeName;
    private final String appEngineTypeName;
    private final int installPriority;

    ComponentType(String typeName, String appEngineTypeName, int installPriority) {
        this.typeName = typeName;
        this.appEngineTypeName = appEngineTypeName;
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

    public String getAppEngineTypeName() {
        return appEngineTypeName;
    }

    public static ComponentType getComponentTypeFromTypeName(String typeName) {
        return Arrays.stream(ComponentType.values())
                .filter(c -> c.getTypeName().equals(typeName))
                .findFirst()
                .orElseThrow(() -> {
                    log.warn("error retrieving a valid ComponentType for:'{}'", typeName);
                    throw new IllegalArgumentException(
                            String.format("error retrieving a valid ComponentType for:'%s'", typeName));
                });
    }

}
