//package org.entando.kubernetes.config.security;
//
//import static org.assertj.core.api.Java6Assertions.assertThat;
//import static org.mockito.Mockito.when;
//
//import java.net.MalformedURLException;
//import java.util.Map;
//import org.entando.kubernetes.config.security.MultipleIdps;
//import org.entando.kubernetes.config.security.MultipleIdps.OAuth2IdpConfig;
//import org.entando.kubernetes.security.oauth2.JwtAuthorityExtractor;
//import org.entando.kubernetes.stubhelper.TenantConfigStubHelper;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Tag;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.springframework.security.authentication.AuthenticationManager;
//
//@Tag("unit")
//@ExtendWith(MockitoExtension.class)
//class AuthenticationManagerConfigTest {
//
//    @Mock
//    private JwtAuthorityExtractor jwtAuthorityExtractor;
//
//    @Mock
//    private MultipleIdps multipleIdps;
//
//    private AuthenticationManagerConfig authenticationManagerConfig;
//
//    @BeforeEach
//    void setUp() {
//        authenticationManagerConfig = new AuthenticationManagerConfig(jwtAuthorityExtractor);
//    }
//
//    @Test
//    void testAuthenticationManagersCreation() throws MalformedURLException {
//        final OAuth2IdpConfig oAuth2IdpConfig1 = TenantConfigStubHelper.stubOAuth2IdpConfig("1");
//        final OAuth2IdpConfig oAuth2IdpConfig2 = TenantConfigStubHelper.stubOAuth2IdpConfig("2");
//        when(multipleIdps.getTrustedIssuers()).thenReturn(Map.of(
//                "issuer1", oAuth2IdpConfig1, "issuer2", oAuth2IdpConfig2));
//        when(multipleIdps.getIdpConfigForIssuer("issuer1")).thenReturn(oAuth2IdpConfig1);
//        when(multipleIdps.getIdpConfigForIssuer("issuer2")).thenReturn(oAuth2IdpConfig2);
//
//        // Mocking NimbusJwtDecoder creation
//        //when(multipleIdps.getIdpConfigForIssuer("issuer1").getJwkUri()).thenReturn("https://dummyjwk1.com");
//        //when(multipleIdps.getIdpConfigForIssuer("issuer2").getJwkUri()).thenReturn("https://dummyjwk2.com");
//
//        final Map<String, AuthenticationManager> stringAuthenticationManagerMap = authenticationManagerConfig.authenticationManagers(
//                multipleIdps);
//
//        assertThat(authenticationManagerConfig.authenticationManagers(multipleIdps)).hasSize(2);
//
//        //stringAuthenticationManagerMap.get("issuer1").
//
////        AuthenticationManagerBuilder authenticationManagerBuilder = mock(AuthenticationManagerBuilder.class);
////        when(authenticationManagerBuilder.userDetailsService(any())).thenReturn(authenticationManagerBuilder);
////
////        // Creating instance of AuthenticationManagerConfig and calling the method
////        authenticationManagerConfig.authenticationManagers(multipleIdps);
//
//        // Add assertions or verifications as needed
//    }
//
//}