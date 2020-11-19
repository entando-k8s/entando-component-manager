package org.entando.kubernetes.model.web.request;

public enum FilterOperator {
    LIKE("like"),
    EQUAL("eq"),
    NOT_EQUAL("not"),
    GREATER("gt"),
    LOWER("lt");

    private final String value;

    private FilterOperator(final String value) {
        this.value = value;
    }

    public static FilterOperator parse(String op) {
        final FilterOperator[] values = values();
        for (FilterOperator value : values) {
            if (value.getValue().equals(op)) {
                return value;
            }
        }
        throw new IllegalArgumentException(op + " is not a supported filter operator");
    }

    public String getValue() {
        return this.value;
    }
}