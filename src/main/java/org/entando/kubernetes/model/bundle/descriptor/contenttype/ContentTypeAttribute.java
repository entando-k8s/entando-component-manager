package org.entando.kubernetes.model.bundle.descriptor.contenttype;

import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.entando.kubernetes.model.bundle.descriptor.Role;

@Getter
@Setter
public class ContentTypeAttribute {

    private String code;
    private String type;
    private Map<String, String> names;
    private List<Role> roles;
    private List<String> disablingCodes;
    private Boolean mandatory;
    private Boolean listFilter;
    private Boolean indexable;

    private String enumeratorExtractorBean;
    private String enumeratorStaticItems;
    private String enumeratorStaticItemsSeparator;

    private ContentTypeValidationRule validationRules;

    private ContentTypeAttribute nestedAttribute;
    private List<ContentTypeAttribute> compositeAttributes;

}
