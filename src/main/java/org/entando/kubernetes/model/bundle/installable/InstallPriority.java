package org.entando.kubernetes.model.bundle.installable;

public enum InstallPriority {
    PLUGIN(0),
    DIRECTORY(1),
    LABEL(2),
    ASSET(3),
    WIDGET(4),
    CONTENT_TYPE(5),
    CONTENT_TEMPLATE(6),
    FRAGMENT(7),
    PAGE_TEMPLATE(8),
    PAGE(9);

    private int priority;

    InstallPriority(int priority) {
        this.priority = priority;
    }

    public int getPriority() {
        return priority;
    }
}
