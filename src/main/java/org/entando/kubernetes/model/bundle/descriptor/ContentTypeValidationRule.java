package org.entando.kubernetes.model.bundle.descriptor;

import lombok.Getter;
import lombok.Setter;

@Getter@Setter
public class ContentTypeValidationRule {

    private String minLength;
    private String maxLength;
    private String equalNumber;

    private String regex;
    private String rangeStartString;
    private String rangeEndString;
    private String rangeStartStringAttribute;
    private String rangeEndStringAttribute;
    private String equalString;
    private String equalStringAttribute;
    private String rangeStartDate;
    private String rangeEndDate;
    private String rangeStartDateAttribute;
    private String rangeEndDateAttribute;
    private String equalDate;
    private String equalDateAttribute;
    private String rangeStartNumber;
    private String rangeStartNumberAttribute;
    private String rangeEndNumber;
    private String rangeEndNumberAttribute;
    private String equalNumberAttribute;
    private OgnlValidation ognlValidation;

}
