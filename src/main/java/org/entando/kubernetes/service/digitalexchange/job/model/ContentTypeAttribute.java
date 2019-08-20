package org.entando.kubernetes.service.digitalexchange.job.model;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter@Setter
public class ContentTypeAttribute {

    private String code;
    private String type;
    private String name;
    private List<Role> roles;
    private List<String> disablingCodes;
    private Boolean mandatory;
    private Boolean listFilter;
    private Boolean indexable;

    private String enumeratorExtractorBean;
    private String enumeratorStaticItems;
    private String enumeratorStaticItemsSeparator;

    // commented due to lack documentation
    // private List<CompositeAttribute> compositeAttributes;

    private ContentTypeValidationRule validationRules;


}
