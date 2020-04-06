package org.entando.kubernetes.model.bundle.descriptor;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OgnlValidation {

    private Boolean applyOnlyToFilledAttr;

    private String errorMessage;
    private String helpMessage;
    private String keyForErrorMessage;
    private String keyForHelpMessage;
    private String ognlExpression;

}
