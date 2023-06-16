package org.entando.kubernetes.security.oauth2;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.entando.kubernetes.config.tenant.TenantConfig;
import org.entando.kubernetes.config.tenant.TenantContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.stereotype.Component;

@Component
public class JwtAuthorityExtractor extends JwtAuthenticationConverter {

    @Value("${spring.security.oauth2.client.registration.oidc.client-id}")
    public String clientId;
    
    @Autowired(required = false)
    private Map<String, TenantConfig> tenantConfigs;

    @Override
    protected Collection<GrantedAuthority> extractAuthorities(Jwt jwt) {
        return this.extractAuthorityFromClaims(jwt.getClaims());
    }

    public List<GrantedAuthority> extractAuthorityFromClaims(Map<String, Object> claims) {
        return mapRolesToGrantedAuthorities(
                getRolesFromClaims(claims));
    }

    @SuppressWarnings("unchecked")
    private Collection<String> getRolesFromClaims(Map<String, Object> claims) {
        Map<String, Object> resourceAccessClaim = (Map<String, Object>) claims
                .getOrDefault("resource_access", new HashMap<>());
        String currentTenant = TenantContext.getCurrentTenant();
        String currentClientId = (null == this.tenantConfigs || StringUtils.isBlank(currentTenant)) ? this.clientId : 
                Optional.ofNullable(this.tenantConfigs.get(currentTenant)).map(c -> c.getDeKcClientId()).orElse(clientId); 
        if (resourceAccessClaim.containsKey(currentClientId)) {
            return (Collection<String>) ((Map) resourceAccessClaim.get(currentClientId))
                    .getOrDefault("roles", new ArrayList<>());
        } else {
            return new ArrayList<>();
        }
    }

    private List<GrantedAuthority> mapRolesToGrantedAuthorities(Collection<String> roles) {
        return roles.stream().map(SimpleGrantedAuthority::new).collect(Collectors.toList());
    }
    
}
