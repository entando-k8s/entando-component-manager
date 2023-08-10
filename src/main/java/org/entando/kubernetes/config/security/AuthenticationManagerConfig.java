package org.entando.kubernetes.config.security;

import static org.springframework.security.oauth2.jwt.JwtClaimNames.ISS;

import com.nimbusds.jose.KeySourceException;
import com.nimbusds.jose.jwk.source.DefaultJWKSetCache;
import com.nimbusds.jose.jwk.source.RemoteJWKSet;
import com.nimbusds.jose.proc.JWSAlgorithmFamilyJWSKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.entando.kubernetes.config.security.MultipleIdps.OAuth2IdpConfig;
import org.entando.kubernetes.config.tenant.thread.TenantContextHolder;
import org.entando.kubernetes.security.oauth2.JwtAuthorityExtractor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationProvider;

@Configuration
@Slf4j
public class AuthenticationManagerConfig {

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
        var jwtDecoder = new NimbusJwtDecoder(this.configureJwksCache(config));
        jwtDecoder.setJwtValidator(this.createValidators(config));
        var authenticationProvider = new JwtAuthenticationProvider(jwtDecoder);
        authenticationProvider.setJwtAuthenticationConverter(jwtAuthorityExtractor);
        return authenticationProvider;
    }

    private DefaultJWTProcessor<SecurityContext> configureJwksCache(OAuth2IdpConfig config) {
        try {
            var jwkSetCache = new DefaultJWKSetCache(
                    config.getJwkCacheTtl().toMinutes(),
                    config.getJwkCacheRefresh().toMinutes(),
                    TimeUnit.MINUTES);
            var jwsKeySelector = JWSAlgorithmFamilyJWSKeySelector.fromJWKSource(
                    new RemoteJWKSet<>(new URL(config.getJwkUri()), null, jwkSetCache));

            var jwtProcessor = new DefaultJWTProcessor<>();
            jwtProcessor.setJWSKeySelector(jwsKeySelector);
            return jwtProcessor;
        } catch (KeySourceException | MalformedURLException e) {
            throw new IllegalArgumentException(e.getMessage());
        }
    }

    private DelegatingOAuth2TokenValidator<Jwt> createValidators(OAuth2IdpConfig config) {
        return new DelegatingOAuth2TokenValidator<>(JwtValidators.createDefaultWithIssuer(config.getIssuerUri()),
                createTenantIssuerValidator(config));
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
