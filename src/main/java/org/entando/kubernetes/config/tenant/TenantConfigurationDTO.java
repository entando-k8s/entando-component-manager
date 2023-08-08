package org.entando.kubernetes.config.tenant;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class TenantConfigurationDTO {

    @JsonProperty("tenantCode")
    private String tenantCode;

    @JsonProperty("dbMaxTotal")
    private int dbMaxTotal;

    @JsonProperty("initializationAtStartRequired")
    private boolean initializationAtStartRequired;

    @JsonProperty("fqdns")
    private String fqdns;

    @JsonProperty("kcEnabled")
    private boolean kcEnabled;

    @JsonProperty("kcAuthUrl")
    private String kcAuthUrl;

    @JsonProperty("kcRealm")
    private String kcRealm;

    @JsonProperty("kcClientId")
    private String kcClientId;

    @JsonProperty("kcClientSecret")
    private String kcClientSecret;

    @JsonProperty("kcPublicClientId")
    private String kcPublicClientId;

    @JsonProperty("kcSecureUris")
    private String kcSecureUris;

    @JsonProperty("kcDefaultAuthorizations")
    private String kcDefaultAuthorizations;

    @JsonProperty("dbDriverClassName")
    private String dbDriverClassName;

    @JsonProperty("dbUrl")
    private String dbUrl;

    @JsonProperty("dbUsername")
    private String dbUsername;

    @JsonProperty("dbPassword")
    private String dbPassword;

    @JsonProperty("cdsPublicUrl")
    private String cdsPublicUrl;

    @JsonProperty("cdsPrivateUrl")
    private String cdsPrivateUrl;

    @JsonProperty("cdsPath")
    private String cdsPath;

    @JsonProperty("solrAddress")
    private String solrAddress;

    @JsonProperty("solrCore")
    private String solrCore;

    @JsonProperty("deDbDriverClassName")
    private String deDbDriverClassName;

    @JsonProperty("deDbUrl")
    private String deDbUrl;

    @JsonProperty("deDbUsername")
    private String deDbUsername;

    @JsonProperty("deDbPassword")
    private String deDbPassword;

    @JsonProperty("deKcClientId")
    private String deKcClientId;

    @JsonProperty("deKcClientSecret")
    private String deKcClientSecret;
}
