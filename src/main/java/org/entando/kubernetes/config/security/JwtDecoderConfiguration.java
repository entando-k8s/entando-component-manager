package org.entando.kubernetes.config.security;

// https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/context/annotation/Profile.html
// the profile are by default in OR we need AND
//@Profile("!test & !test-db")
//@Configuration
//public class JwtDecoderConfiguration {
//
//    @Bean
//    JwtDecoder jwtDecoder(@Value("${spring.security.oauth2.client.provider.oidc.issuer-uri}") String issuerUri) {
//        NimbusJwtDecoder jwtDecoder = (NimbusJwtDecoder)
//                JwtDecoders.fromOidcIssuerLocation(issuerUri);
//
//        OAuth2TokenValidator<Jwt> audienceValidator = new AudienceValidator();
//        OAuth2TokenValidator<Jwt> withIssuer = JwtValidators.createDefaultWithIssuer(issuerUri);
//        OAuth2TokenValidator<Jwt> withAudience = new DelegatingOAuth2TokenValidator<>(withIssuer, audienceValidator);
//
//        jwtDecoder.setJwtValidator(withAudience);
//
//        return jwtDecoder;
//    }
//}
