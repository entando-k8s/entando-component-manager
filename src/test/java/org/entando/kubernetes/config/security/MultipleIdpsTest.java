package org.entando.kubernetes.config.security;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import lombok.SneakyThrows;
import org.entando.kubernetes.config.tenant.TenantConfigDTO;
import org.entando.kubernetes.stubhelper.TenantConfigStubHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class MultipleIdpsTest {

    private TenantConfigDTO tenantConfig1;
    private TenantConfigDTO tenantConfig2;
    private MultipleIdps multipleIdps;

    @BeforeEach
    void setUp() {
        tenantConfig1 = mockTenantConfigDTO("1");
        tenantConfig2 = mockTenantConfigDTO("2");
        List<TenantConfigDTO> tenantConfigs = Arrays.asList(tenantConfig1, tenantConfig2);

        multipleIdps = new MultipleIdps(tenantConfigs);
    }

    @Test
    void shouldCorrectlyIdentifyTrustedIssuers() {
        assertThat(multipleIdps.isTrustedIssuer(TenantConfigStubHelper.ISSUER_URI + "1/realms/myreaml1")).isTrue();
        assertThat(multipleIdps.isTrustedIssuer(TenantConfigStubHelper.ISSUER_URI + "2/realms/myreaml2")).isTrue();
        assertThat(multipleIdps.isTrustedIssuer("nonexistent")).isFalse();
    }

    @Test
    void shouldCorrectlyGetIdpConfigForIssuer() {
        assertThat(multipleIdps.getIdpConfigForIssuer(TenantConfigStubHelper.ISSUER_URI + "1/realms/myreaml1")
                .getTenantCode()).isEqualTo(TenantConfigStubHelper.TENANT_CODE + "1");
        assertThat(multipleIdps.getIdpConfigForIssuer(TenantConfigStubHelper.ISSUER_URI + "2/realms/myreaml2")
                .getTenantCode()).isEqualTo(TenantConfigStubHelper.TENANT_CODE + "2");
        assertThat(multipleIdps.getIdpConfigForIssuer("nonexistent")).isNull();
    }

    @Test
    void testCorrectlyGetIdpConfigForIssuer() {
        assertThat(
                multipleIdps.getIdpConfigForIssuer(TenantConfigStubHelper.ISSUER_URI + "1/realms/myreaml1")
                        .getTenantCode()).isEqualTo(
                tenantConfig1.getTenantCode());
        assertThat(
                multipleIdps.getIdpConfigForIssuer(TenantConfigStubHelper.ISSUER_URI + "2/realms/myreaml2")
                        .getTenantCode()).isEqualTo(
                tenantConfig2.getTenantCode());
        assertThat(multipleIdps.getIdpConfigForIssuer("nonexistent")).isNull();
    }

    @SneakyThrows
    public TenantConfigDTO mockTenantConfigDTO(String suffix) {
        TenantConfigDTO mock = mock(TenantConfigDTO.class);
        when(mock.getKcAuthUrl()).thenReturn(TenantConfigStubHelper.ISSUER_URI + suffix);
        when(mock.getKcRealm()).thenReturn(TenantConfigStubHelper.REALM + suffix);
        when(mock.getTenantCode()).thenReturn(TenantConfigStubHelper.TENANT_CODE + suffix);
        return mock;
    }
}