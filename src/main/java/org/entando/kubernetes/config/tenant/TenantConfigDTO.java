package org.entando.kubernetes.config.tenant;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@EqualsAndHashCode
@ToString(exclude = {"cmDbPassword"})
@JsonIgnoreProperties(ignoreUnknown = true)
public class TenantConfigDTO {

    private String tenantCode;
    private String fqdns;
    
    private String kcAuthUrl;
    private String kcRealm;
    private String kcCmClientId;
    private String kcCmClientSecret;

    private String cmDbJdbcUrl;
    private String cmDbUsername;
    private String cmDbPassword;

}
