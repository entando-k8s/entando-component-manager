package org.entando.kubernetes.config.tenant;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Tag("unit")
public class TenantContextManagerTest {

    @Test
    void getTenantCodeShouldReturnTheCorrectValue() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        String tenantCode = "testTenantCode";
        TenantContextManager.setTenantCode(tenantCode);
        assertThat(TenantContextManager.getTenantCode()).isEqualTo(tenantCode);
    }

}
