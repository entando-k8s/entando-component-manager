package org.entando.kubernetes.config.security;

import static org.springframework.security.oauth2.jwt.JwtClaimNames.ISS;

import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.entando.kubernetes.config.security.MultipleIdps.OAuth2IdpConfig;
import org.entando.kubernetes.config.tenant.thread.TenantContextHolder;
import org.entando.kubernetes.security.oauth2.AudienceValidator;
import org.entando.kubernetes.security.oauth2.JwtAuthorityExtractor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationProvider;

@Configuration
@Slf4j
public class AuthenticationManagerConfiguration {

    @Bean
    public Map<String, AuthenticationManager> authenticationManagers(MultipleIdps multipleIdps,
                                                                     JwtAuthorityExtractor jwtAuthorityExtractor) {
        return multipleIdps.getTrustedIssuers().keySet()
                .stream()
                .collect(Collectors.toMap(issuer -> issuer,
                        issuer -> {
                            log.info("Loading authentication manager for issuer: '{}'", issuer);
                            return createJwtAuthProvider(multipleIdps
                                    .getIdpConfigForIssuer(issuer), jwtAuthorityExtractor)::authenticate;
                        }));
    }

    private JwtAuthenticationProvider createJwtAuthProvider(OAuth2IdpConfig config,
                                                            JwtAuthorityExtractor jwtAuthorityExtractor) {
        var jwtDecoder = buildJwtDecoder(config);
        var authenticationProvider = new JwtAuthenticationProvider(jwtDecoder);
        authenticationProvider.setJwtAuthenticationConverter(jwtAuthorityExtractor);
        return authenticationProvider;
    }

    JwtDecoder buildJwtDecoder(OAuth2IdpConfig config) {
        log.debug("build jwt decoder for issued uri {}", config.getIssuerUri());
        NimbusJwtDecoder jwtDecoder = JwtDecoders.fromOidcIssuerLocation(config.getIssuerUri());
        OAuth2TokenValidator<Jwt> withAudience = buildValidators(config);
        jwtDecoder.setJwtValidator(withAudience);

        return jwtDecoder;
    }

    private DelegatingOAuth2TokenValidator<Jwt> buildValidators(OAuth2IdpConfig config) {
        OAuth2TokenValidator<Jwt> audienceValidator = new AudienceValidator();
        OAuth2TokenValidator<Jwt> withStandardIssuer = JwtValidators.createDefaultWithIssuer(config.getIssuerUri());
        OAuth2TokenValidator<Jwt> withTenantIssuer = createTenantIssuerValidator(config);
        return new DelegatingOAuth2TokenValidator<>(withStandardIssuer, withTenantIssuer, audienceValidator);
    }

    /**
     * create a JwtClaimValidator to compares the issuer of the JWT and the one extracted by the current tenant config.
     *
     * @return the created JwtClaimValidator
     */
    private JwtClaimValidator<String> createTenantIssuerValidator(OAuth2IdpConfig config) {
        return new JwtClaimValidator<>(ISS,
                iss -> {
                    log.debug(
                            "Request received by tenant: '{}'. Issuer extracted by JWT: '{}' - tenant got by config: '{}'",
                            TenantContextHolder.getCurrentTenantCode(), iss, config.getTenantCode());
                    return StringUtils.equals(TenantContextHolder.getCurrentTenantCode(), config.getTenantCode());
                });
    }
}
