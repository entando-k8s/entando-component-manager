package org.entando.kubernetes.validator;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

import java.net.URL;
import java.util.List;
import java.util.stream.Stream;
import org.assertj.core.api.Assertions;
import org.entando.kubernetes.exception.EntandoComponentManagerException;
import org.entando.kubernetes.exception.EntandoValidationException;
import org.entando.kubernetes.stubhelper.BundleStubHelper;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
class ValidationFunctionsTest {

    private final String emptyMex = "empty";
    private final String invalidMex = "not valid";

    @Test
    void shouldThrowTheExpectedExceptionIfUrlIsEmpty() {

        final EntandoValidationException entandoValidationException = assertThrows(EntandoValidationException.class,
                () -> ValidationFunctions.composeUrlOrThrow(null, emptyMex, invalidMex));

        assertThat(entandoValidationException.getMessage()).isEqualTo(emptyMex);
    }

    @Test
    void shouldThrowTheExpectedExceptionIfUrlIsNotCompliant() {

        Stream.of("", "ftp://entando.com", "http://", "https://", "https://.com", "http://.com", "https://my-domain-",
                        "https://my-domain.", "http:// ", "http://com.", "http://.com")
                .forEach(urlString -> {
                    try {
                        assertThrows(EntandoValidationException.class,
                                () -> ValidationFunctions.composeUrlOrThrow(urlString, emptyMex, invalidMex),
                                urlString);
                    } catch (Exception e) {
                        fail(e.getMessage());
                    }
                });
    }

    @Test
    void shouldNotThrowExceptionWhenUrlIsCompliant() {

        Stream.of("https://www.mydomain.com?myparam=value&seconp=myval", "http://www.entando.com",
                        "https://www.entando.com", "http://www.entando.com:80",
                        "https://www.ENtando.com", "http://www.enta-ndo.com:092/3-4/ci-one/asdk-a/",
                        "http://www.en_ta-ndo.com:092/3_a/", "http://www.enta-ndo.com:092/3-",
                        "http://www.en_ta-ndo.com:092/ad-/a/a__SDAda", "https://my-domain", "https://localhost",
                        "https://www.mydomain.com/?myparam=value", "https://www.mydomain.com?myparam=value&seconp=myval",
                        "http://www.enta-ndo.com:123456", "http://www.entando.com/my.sec", "https://localhost/")
                .forEach(urlString -> {
                    URL url = ValidationFunctions.composeUrlOrThrow(urlString, emptyMex, invalidMex);
                    assertThat(url.toString()).isEqualTo(urlString);
                });
    }

    @Test
    void shouldReplaceGitAndSshProtocolWithHttpAndShouldNotThrowException() {
        List<String> testCasesList = List.of(
                "git@github.com:entando/my_bundle.git",
                "git://github.com/entando/my_bundle.git",
                "ssh://github.com/entando/my_bundle.git",
                "git@github.com:entando:my_bundle.git");

        testCasesList.forEach(url -> {
            String actual = ValidationFunctions.composeUrlForcingHttpProtocolOrThrow(url, "null", "not valid");
            Assertions.assertThat(actual).isEqualTo(url);
        });
    }

    @Test
    void shouldReturnTheSameStringWhenProtocolIsNotOneOfTheExpected() {
        List<String> testCasesList = List.of(
                "got@github.com/entando/my_bundle.git",
                "got://github.com/entando/my_bundle.git",
                "sssh://github.com/entando/my_bundle.git",
                "ftp://github.com/entando/my_bundle.git");

        testCasesList.forEach(url ->
                assertThrows(EntandoValidationException.class,
                        () -> ValidationFunctions.composeUrlForcingHttpProtocolOrThrow(url, "null", "not valid")));
    }

    @Test
    void shouldCorrectlyValidateAnEntityCode() {
        assertThrows(EntandoComponentManagerException.class, () -> ValidationFunctions.validateEntityCodeOrThrow(null));
        assertThrows(EntandoComponentManagerException.class, () -> ValidationFunctions.validateEntityCodeOrThrow(""));
        assertThrows(EntandoComponentManagerException.class, () -> ValidationFunctions.validateEntityCodeOrThrow("code1234abcd"));
        assertThrows(EntandoComponentManagerException.class, () -> ValidationFunctions.validateEntityCodeOrThrow("-code1234abcd"));
        assertThrows(EntandoComponentManagerException.class, () -> ValidationFunctions.validateEntityCodeOrThrow("code1234abcd-"));
        assertThrows(EntandoComponentManagerException.class, () -> ValidationFunctions.validateEntityCodeOrThrow("code-1234abcm"));

        final String code = ValidationFunctions.validateEntityCodeOrThrow(BundleStubHelper.BUNDLE_CODE);
        assertThat(code).isEqualTo(BundleStubHelper.BUNDLE_CODE);
    }

    @Test
    void testFQDN() {
        final String INVALID_FQDN1 = "tenant1.mt720.k8s-domain.o rg";

        assertThat(ValidationFunctions.validateURL(INVALID_FQDN1,
                false, false, false)).isFalse();

        assertThat(ValidationFunctions.validateFQDN(INVALID_FQDN1)).isFalse();

        final String INVALID_FQDN2 = "jdbc:postgresql://default-postgresql-dbms-in-namespace-service.test-mt-720.svc.cluster.local:5432/tenant1";
        assertThat(ValidationFunctions.validateURL(INVALID_FQDN2,
                true, true, true)).isFalse();

        final String INVALID_FQDN3 = "tenant1.mt720.k8s-domain.org:123q2/tenant1";
        assertThat(ValidationFunctions.validateURL(INVALID_FQDN3,
                false, true, true)).isFalse();
        assertThat(ValidationFunctions.validateURL(INVALID_FQDN3,
                false, false, true)).isFalse();

        assertThat(ValidationFunctions.validateFQDN(INVALID_FQDN3)).isFalse();

        final String INVALID_FQDN4 = "http://tenant1";
        assertThat(ValidationFunctions.validateFQDN(INVALID_FQDN4)).isFalse();

        final String INVALID_FQDN5 = "tenant1";
        assertThat(ValidationFunctions.validateFQDN(INVALID_FQDN5)).isFalse();

        final String INVALID_FQDN6 = ".com";
        assertThat(ValidationFunctions.validateFQDN(INVALID_FQDN6)).isFalse();

        final String INVALID_FQDN7 = "mock-url";
        assertThat(ValidationFunctions.validateFQDN(INVALID_FQDN7)).isFalse();

        final String INVALID_FQDN8 = "8-8-8-8.nip.io";
        assertThat(ValidationFunctions.validateFQDN(INVALID_FQDN8)).isTrue();

        final String INVALID_FQDN9 = "192.168.1.234";
        assertThat(ValidationFunctions.validateFQDN(INVALID_FQDN9)).isFalse();

    }

    @Test
    void testUrl5() {
        final String VALID_URL5 = "https://cds-mt720.k8s-domain.org:2677/tenant1/";
        assertThat(ValidationFunctions.validateURL(VALID_URL5,
                true, true, true)).isTrue();
        assertThat(ValidationFunctions.validateURL(VALID_URL5,
                false, false, false)).isTrue();
        assertThat(ValidationFunctions.validateURL(VALID_URL5,
                true, false, false)).isTrue();
        assertThat(ValidationFunctions.validateURL(VALID_URL5,
                false, true, false)).isTrue();
        assertThat(ValidationFunctions.validateURL(VALID_URL5,
                false, false, true)).isTrue();

        assertThat(ValidationFunctions.validateFQDN(VALID_URL5)).isFalse();
    }

    @Test
    void testUrl4() {
        final String VALID_URL4 = "https://cds-mt720.k8s-domain.org/tenant1/";
        assertThat(ValidationFunctions.validateURL(VALID_URL4,
                true, false, false)).isTrue();
        assertThat(ValidationFunctions.validateURL(VALID_URL4,
                true, false, true)).isTrue();
        assertThat(ValidationFunctions.validateURL(VALID_URL4,
                true, true, true)).isFalse();
        assertThat(ValidationFunctions.validateURL(VALID_URL4,
                false, false, false)).isTrue();

        assertThat(ValidationFunctions.validateFQDN(VALID_URL4)).isFalse();
    }

    @Test
    void testUrl3() {
        // with protocol and port
        final String VALID_URL3 = "http://mt720-cds-tenant1-service.test-mt-720.svc.cluster.local:8080";
        assertThat(ValidationFunctions.validateURL(VALID_URL3,
                true, true, false)).isTrue();
        assertThat(ValidationFunctions.validateURL(VALID_URL3,
                false, false, true)).isFalse();

        assertThat(ValidationFunctions.validateFQDN(VALID_URL3)).isFalse();
    }

    @Test
    void testUrl2() {
        // with protocol
        final String VALID_URL2 = "https://mt720.k8s-domain.org/auth";
        assertThat(ValidationFunctions.validateURL(VALID_URL2,
                true, false, true)).isTrue();
        assertThat(ValidationFunctions.validateURL(VALID_URL2,
                false, false, true)).isTrue();
        assertThat(ValidationFunctions.validateURL(VALID_URL2,
                false, false, false)).isTrue();
        assertThat(ValidationFunctions.validateURL(VALID_URL2,
                false, true, false)).isFalse();

        assertThat(ValidationFunctions.validateFQDN(VALID_URL2)).isFalse();
    }

    @Test
    void testUrl1() {
        final String VALID_URL1 = "tenant1.mt720.k8s-domain.org";

        // w/o protocol
        assertThat(ValidationFunctions.validateURL(VALID_URL1,
                false, false, false)).isTrue();
        assertThat(ValidationFunctions.validateURL(VALID_URL1,
                true, false, false)).isFalse();
        assertThat(ValidationFunctions.validateURL(VALID_URL1,
                true, true, false)).isFalse();
        assertThat(ValidationFunctions.validateURL(VALID_URL1,
                false, false, true)).isFalse();

        assertThat(ValidationFunctions.validateFQDN(VALID_URL1)).isTrue();
    }

    @Test
    void testFqdnLength() {
        final String INVALID_FQDN_6 = "m3SxbwmmVz7RWyALTCBfwlteCAHHMnStV3c3LYeN9aZUylhsMtFxoC0lW3Y42N3taxCccgRZteESuQsvH8N1iQ6xlEzj7eUH8oPfG1YtjtNsqIxapuvt5DmLuMPTCpXbqw9IRinuJJGl9Auo8Di2MSOxHt3FmNmclPDoCmbnJMuGjVxgTboxa8SrRRZZXbn5UNHK1pSVygo2T6Oemv4E7VI0yqAOXDTuobclq5BGNc3IsLAblzYSPucNA5joH.com";
        assertThat(ValidationFunctions.validateFQDN(INVALID_FQDN_6)).isFalse();
    }

}
