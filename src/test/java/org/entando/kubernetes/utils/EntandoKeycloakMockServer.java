package org.entando.kubernetes.utils;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.springframework.cloud.contract.wiremock.WireMockSpring.options;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;

public class EntandoKeycloakMockServer {


    public static WireMockServer createServer(int port) {
        WireMockServer wireMockServer = new WireMockServer(options().port(port));
        return wireMockServer;
    }

    public static void addKeycloakOidcEndpoints(WireMockServer wireMockServer, int port, String realm) {
        wireMockServer.stubFor(WireMock.get(urlEqualTo("/auth/realms/entando/.well-known/openid-configuration"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody("{\"issuer\":\"http://localhost:" + port + "/auth/realms/" + realm + "\","
                                + "\"authorization_endpoint\":\"http://localhost:" + port + "/auth/realms/" + realm + "/protocol/openid-connect/auth\","
                                + "\"token_endpoint\":\"http://localhost:8099/auth/realms/entando/protocol/openid-connect/token\","
                                + "\"introspection_endpoint\":\"http://localhost:8099/auth/realms/entando/protocol/openid-connect/token/introspect\","
                                + "\"userinfo_endpoint\":\"http://ent.10.214.197.61.nip.io/auth/realms/entando/protocol/openid-connect/userinfo\","
                                + "\"end_session_endpoint\":\"http://ent.10.214.197.61.nip.io/auth/realms/entando/protocol/openid-connect/logout\","
                                + "\"frontchannel_logout_session_supported\":true,\"frontchannel_logout_supported\":true,"
                                + "\"jwks_uri\":\"http://localhost:" + port + "/auth/realms/" + realm + "/protocol/openid-connect/certs\","
                                + "\"check_session_iframe\":\"http://ent.10.214.197.61.nip.io/auth/realms/entando/protocol/openid-connect/login-status-iframe.html\","
                                + "\"grant_types_supported\":[\"authorization_code\",\"implicit\",\"refresh_token\",\"password\",\"client_credentials\",\"urn:ietf:params:oauth:grant-type:device_code\",\"urn:openid:params:grant-type:ciba\"],\"acr_values_supported\":[\"0\",\"1\"],\"response_types_supported\":[\"code\",\"none\",\"id_token\",\"token\",\"id_token token\",\"code id_token\",\"code token\",\"code id_token token\"],\"subject_types_supported\":[\"public\",\"pairwise\"],\"id_token_signing_alg_values_supported\":[\"PS384\",\"ES384\",\"RS384\",\"HS256\",\"HS512\",\"ES256\",\"RS256\",\"HS384\",\"ES512\",\"PS256\",\"PS512\",\"RS512\"],\"id_token_encryption_alg_values_supported\":[\"RSA-OAEP\",\"RSA-OAEP-256\",\"RSA1_5\"],\"id_token_encryption_enc_values_supported\":[\"A256GCM\",\"A192GCM\",\"A128GCM\",\"A128CBC-HS256\",\"A192CBC-HS384\",\"A256CBC-HS512\"],\"userinfo_signing_alg_values_supported\":[\"PS384\",\"ES384\",\"RS384\",\"HS256\",\"HS512\",\"ES256\",\"RS256\",\"HS384\",\"ES512\",\"PS256\",\"PS512\",\"RS512\",\"none\"],\"userinfo_encryption_alg_values_supported\":[\"RSA-OAEP\",\"RSA-OAEP-256\",\"RSA1_5\"],\"userinfo_encryption_enc_values_supported\":[\"A256GCM\",\"A192GCM\",\"A128GCM\",\"A128CBC-HS256\",\"A192CBC-HS384\",\"A256CBC-HS512\"],\"request_object_signing_alg_values_supported\":[\"PS384\",\"ES384\",\"RS384\",\"HS256\",\"HS512\",\"ES256\",\"RS256\",\"HS384\",\"ES512\",\"PS256\",\"PS512\",\"RS512\",\"none\"],\"request_object_encryption_alg_values_supported\":[\"RSA-OAEP\",\"RSA-OAEP-256\",\"RSA1_5\"],\"request_object_encryption_enc_values_supported\":[\"A256GCM\",\"A192GCM\",\"A128GCM\",\"A128CBC-HS256\",\"A192CBC-HS384\",\"A256CBC-HS512\"],\"response_modes_supported\":[\"query\",\"fragment\",\"form_post\",\"query.jwt\",\"fragment.jwt\",\"form_post.jwt\",\"jwt\"],\"registration_endpoint\":\"http://ent.10.214.197.61.nip.io/auth/realms/entando/clients-registrations/openid-connect\",\"token_endpoint_auth_methods_supported\":[\"private_key_jwt\",\"client_secret_basic\",\"client_secret_post\",\"tls_client_auth\",\"client_secret_jwt\"],\"token_endpoint_auth_signing_alg_values_supported\":[\"PS384\",\"ES384\",\"RS384\",\"HS256\",\"HS512\",\"ES256\",\"RS256\",\"HS384\",\"ES512\",\"PS256\",\"PS512\",\"RS512\"],\"introspection_endpoint_auth_methods_supported\":[\"private_key_jwt\",\"client_secret_basic\",\"client_secret_post\",\"tls_client_auth\",\"client_secret_jwt\"],\"introspection_endpoint_auth_signing_alg_values_supported\":[\"PS384\",\"ES384\",\"RS384\",\"HS256\",\"HS512\",\"ES256\",\"RS256\",\"HS384\",\"ES512\",\"PS256\",\"PS512\",\"RS512\"],\"authorization_signing_alg_values_supported\":[\"PS384\",\"ES384\",\"RS384\",\"HS256\",\"HS512\",\"ES256\",\"RS256\",\"HS384\",\"ES512\",\"PS256\",\"PS512\",\"RS512\"],\"authorization_encryption_alg_values_supported\":[\"RSA-OAEP\",\"RSA-OAEP-256\",\"RSA1_5\"],\"authorization_encryption_enc_values_supported\":[\"A256GCM\",\"A192GCM\",\"A128GCM\",\"A128CBC-HS256\",\"A192CBC-HS384\",\"A256CBC-HS512\"],\"claims_supported\":[\"aud\",\"sub\",\"iss\",\"auth_time\",\"name\",\"given_name\",\"family_name\",\"preferred_username\",\"email\",\"acr\"],\"claim_types_supported\":[\"normal\"],\"claims_parameter_supported\":true,\"scopes_supported\":[\"openid\",\"email\",\"offline_access\",\"roles\",\"acr\",\"microprofile-jwt\",\"address\",\"phone\",\"profile\",\"web-origins\"],\"request_parameter_supported\":true,\"request_uri_parameter_supported\":true,\"require_request_uri_registration\":true,\"code_challenge_methods_supported\":[\"plain\",\"S256\"],\"tls_client_certificate_bound_access_tokens\":true,\"revocation_endpoint\":\"http://ent.10.214.197.61.nip.io/auth/realms/entando/protocol/openid-connect/revoke\",\"revocation_endpoint_auth_methods_supported\":[\"private_key_jwt\",\"client_secret_basic\",\"client_secret_post\",\"tls_client_auth\",\"client_secret_jwt\"],\"revocation_endpoint_auth_signing_alg_values_supported\":[\"PS384\",\"ES384\",\"RS384\",\"HS256\",\"HS512\",\"ES256\",\"RS256\",\"HS384\",\"ES512\",\"PS256\",\"PS512\",\"RS512\"],\"backchannel_logout_supported\":true,\"backchannel_logout_session_supported\":true,\"device_authorization_endpoint\":\"http://ent.10.214.197.61.nip.io/auth/realms/entando/protocol/openid-connect/auth/device\",\"backchannel_token_delivery_modes_supported\":[\"poll\",\"ping\"],\"backchannel_authentication_endpoint\":\"http://ent.10.214.197.61.nip.io/auth/realms/entando/protocol/openid-connect/ext/ciba/auth\",\"backchannel_authentication_request_signing_alg_values_supported\":[\"PS384\",\"ES384\",\"RS384\",\"ES256\",\"RS256\",\"ES512\",\"PS256\",\"PS512\",\"RS512\"],"
                                + "\"require_pushed_authorization_requests\":false,"
                                + "\"pushed_authorization_request_endpoint\":\"http://ent.10.214.197.61.nip.io/auth/realms/entando/protocol/openid-connect/ext/par/request\","
                                + "\"mtls_endpoint_aliases\":{"
                                + "\"token_endpoint\":\"http://ent.10.214.197.61.nip.io/auth/realms/entando/protocol/openid-connect/token\","
                                + "\"revocation_endpoint\":\"http://ent.10.214.197.61.nip.io/auth/realms/entando/protocol/openid-connect/revoke\","
                                + "\"introspection_endpoint\":\"http://ent.10.214.197.61.nip.io/auth/realms/entando/protocol/openid-connect/token/introspect\","
                                + "\"device_authorization_endpoint\":\"http://ent.10.214.197.61.nip.io/auth/realms/entando/protocol/openid-connect/auth/device\",\"registration_endpoint\":\"http://ent.10.214.197.61.nip.io/auth/realms/entando/clients-registrations/openid-connect\","
                                + "\"userinfo_endpoint\":\"http://ent.10.214.197.61.nip.io/auth/realms/entando/protocol/openid-connect/userinfo\","
                                + "\"pushed_authorization_request_endpoint\":\"http://ent.10.214.197.61.nip.io/auth/realms/entando/protocol/openid-connect/ext/par/request\","
                                + "\"backchannel_authentication_endpoint\":\"http://ent.10.214.197.61.nip.io/auth/realms/entando/protocol/openid-connect/ext/ciba/auth\""
                                + "}"
                                + "}")));
    }

    public static void addKeycloakCertEndpoints(WireMockServer wireMockServer, String realm) {

        wireMockServer.stubFor(WireMock.get(urlEqualTo("/auth/realms/" + realm + "/protocol/openid-connect/certs"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody("{\"keys\":[{"
                            + "\"kid\":\"FcxcnWpLOBzR0rEC9VyX8GCAjRaeMwPCFqvBTqgAenk\","
                            + "\"kty\":\"RSA\",\"alg\":\"RS256\","
                            + "\"use\":\"sig\",\"n\":\"luvfYQSi9vpMyFQXj4C7K6C93jerej24yWXlxFJ9tggwU9YgQ_Zr3NU5xehART1T2BDnsBd5uT6MjGZ_yK4V5hQdZdTbemdkbSwZ"
                            + "wpzQNfESl7PvvSmH6kEY95gR5Px84IezjNWqsGzN_gjNs4YM5W6UJl3sbc2BQcJd_aZHHZSKgyktmeQut_twDQ2U-BP1eRo8UsxFEOlvXVs0ZQP8-Ttn4uMMZfAt5_"
                            + "6SBEvit7i_not6_sdgdmx6GFZm8uT9_1kv8eivXmjvOW09OMRyGg-iPzcM5ndyamSirEEftcxQlahBP4ggbpP5JcpowaBmCeNNzAyTunVDEAHoRgVMkw\","
                            + "\"e\":\"AQAB\","
                            + "\"x5c\":[\"MIICnTCCAYUCBgGJltff3zANBgkqhkiG9w0BAQsFADASMRAwDgYDVQQDDAdlbnRhbmRvMB4XDTIzMDcyNzEwMTI1N1oXDTMzMDcyNzEwMTQzN1owE"
                            + "jEQMA4GA1UEAwwHZW50YW5kbzCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAJbr32EEovb6TMhUF4+Auyugvd43q3o9uMll5cRSfbYIMFPWIEP2a9zVOcX"
                            + "oQEU9U9gQ57AXebk+jIxmf8iuFeYUHWXU23pnZG0sGcKc0DXxEpez770ph+pBGPeYEeT8fOCHs4zVqrBszf4IzbOGDOVulCZd7G3NgUHCXf2mRx2UioMpLZnkLrf7c"
                            + "A0NlPgT9XkaPFLMRRDpb11bNGUD/Pk7Z+LjDGXwLef+kgRL4re4v56Lev7HYHZsehhWZvLk/f9ZL/Hor15o7zltPTjEchoPoj83DOZ3cmpkoqxBH7XMUJWoQT+IIG6"
                            + "T+SXKaMGgZgnjTcwMk7p1QxAB6EYFTJMCAwEAATANBgkqhkiG9w0BAQsFAAOCAQEAiuibHDAekzw6J4OntReE62Fvku8YELo1W8eIMgNk7Tt72UZu9f14Sb0QHpDqg"
                            + "P9QJ0M2QQRFW3sE6qWvKyRIHhxVkK2fGWckj1XcwLk7R7J07VjYyEtDR6emrnLUbAkpnBtXHM2OxZYXrf7P/WkHqteZoz5HXUYdQxQVVGVLB+b+6pc2bn00LVOavBG"
                            + "dd9jSxwMpmp/lfJwWq7k2RXwcRr1BZ4vuqPk3FrY/mWKHzOYmlQa0mFM3B8A9Q6Y85rrKbqUVaP0COQE4atyicL4rg2/l1HyUFlTWLelSmiUsEl5Hp7GppNI1xp8R3"
                            + "lDVw0+OReINe9xNi4OdN8KyVdjuXA==\"],"
                            + "\"x5t\":\"mcV7OJ6tiKP6r325ZNU0KL4mPxc\","
                            + "\"x5t#S256\":\"_YX3j8sZ8o_bjJ2dJrBfqvpn53OfrQsiOhERcvbQWN8\"},"
                            + "{\"kid\":\"pu6uNTAJ5YRM1mZ--NyzeaUUx6fE3zptQOwxQ_fAmTQ\","
                            + "\"kty\":\"RSA\",\"alg\":\"RSA-OAEP\","
                            + "\"use\":\"enc\",\"n\":\"gSvWv13duF6uj3qLZZD_5LVRo2qG96VwkpP4OrpaCTYMJsfFwTQTfa8Fe4YiduShi82lS2JzYAzD2_qZbBQldrH-2365g_IICtaGOs9"
                            + "HpyMMBmmC7B-koEgUZ1muClUCM7e-trWXABVqAdeUVX7H_y0WB4xy3U21o04qlgRo7hVPHhRZnb5bKDzyJ1iHPlz0KSww-Wgl89k6fpvRhbrr3X1ODYFqd4deTyNJZ_"
                            + "xOHfSINlk_hmi4vEZIBG360VVJsyZz2kRj6ZsK_jH0Mf-q0r0EVe8K1hpMd5-DtNqnb2-GYbrAp7xzCxwJ-ShvjnfOsAA_XxggQXaDX5uoFcrdMw\","
                            + "\"e\":\"AQAB\","
                            + "\"x5c\":[\"MIICnTCCAYUCBgGJltfhyjANBgkqhkiG9w0BAQsFADASMRAwDgYDVQQDDAdlbnRhbmRvMB4XDTIzMDcyNzEwMTI1N1oXDTMzMDcyNzEwMTQzN1owEjEQ"
                            + "MA4GA1UEAwwHZW50YW5kbzCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAIEr1r9d3bhero96i2WQ/+S1UaNqhvelcJKT+Dq6Wgk2DCbHxcE0E32vBXuGInb"
                            + "koYvNpUtic2AMw9v6mWwUJXax/tt+uYPyCArWhjrPR6cjDAZpguwfpKBIFGdZrgpVAjO3vra1lwAVagHXlFV+x/8tFgeMct1NtaNOKpYEaO4VTx4UWZ2+Wyg88idYhz"
                            + "5c9CksMPloJfPZOn6b0YW66919Tg2BaneHXk8jSWf8Th30iDZZP4ZouLxGSARt+tFVSbMmc9pEY+mbCv4x9DH/qtK9BFXvCtYaTHefg7Tap29vhmG6wKe8cwscCfkob"
                            + "453zrAAP18YIEF2g1+bqBXK3TMCAwEAATANBgkqhkiG9w0BAQsFAAOCAQEAagJOrD4Gb21gpa/EYJVWx7a1cZRnKDq5+7uSnVT6DaB+alchW7jh4R1IkXJNGJqJ2Fdb"
                            + "y5SzgTo+gomGqxR8InJcMRzOapXQsc6AAK6YhdbYImahmMKD6C7z2i7IJ1RtcFPjg7I/Q5qQxO2Fq9qF9c1DyzPm+m9QkolNjZfiummrGGDXLXfbYvx7/zBrqZsVR7s"
                            + "uBpr9k9sUt0dzE+uGh+ELRFmDPCXFJvMWWIkHA6dhDQ7ZCDjZ0NMpg8lycbPu84t83xF+rGhywuulYNsNErG+qYNkLpeNWtpIgzy5dKnTNxWeGoRy4MH7O9n6cvQ/aH"
                            + "Z7C87m8dU69H+xwoNlog==\"],"
                            + "\"x5t\":\"hb6Y_ybQAWaFJv2_X9mqNkyod-w\","
                            + "\"x5t#S256\":\"q79ysJ-AFn7vP2GOeVbM-phHauFZrhNz5dSpSymq2tQ\""
                            + "}]}")));
    }

    public static void startServer(WireMockServer wireMockServer) {
        if (!wireMockServer.isRunning()) {
            wireMockServer.start();
        }
    }

    public static void stopServer(WireMockServer wireMockServer) {
        if (wireMockServer.isRunning()) {
            wireMockServer.stop();
        }
    }

    public static WireMockServer mockAndStartServer(int port, String realm) {
        WireMockServer mock = createServer(port);
        addKeycloakOidcEndpoints(mock, port, realm);
        addKeycloakCertEndpoints(mock, realm);
        startServer(mock);
        return mock;
    }

}
