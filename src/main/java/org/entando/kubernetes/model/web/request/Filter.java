package org.entando.kubernetes.model.web.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class Filter {

    public static final String ASC_ORDER = "ASC";
    public static final String DESC_ORDER = "DESC";

    private String attribute;
    private String entityAttr;
    private String operator;
    private String value;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private String type;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private String order;

    private String[] allowedValues;

    public Filter(final String attribute, final String value) {
        this.attribute = attribute;
        this.value = value;
        this.operator = FilterOperator.EQUAL.getValue();
    }

}
