package org.entando.kubernetes.config.tenant;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.util.Collections;
import javax.servlet.ServletException;
import org.entando.kubernetes.config.tenant.thread.TenantContextHolder;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;


@Tag("unit")
class TenantFilterTest {

    @Test
    void shouldSetCorrectTwnantWithExistingXEntandoCustomHeader() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-ENTANDO-TENANTCODE", "tenant2");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();
        try (MockedStatic<TenantContextHolder> tenantContextHolder = Mockito
                .mockStatic(TenantContextHolder.class, Mockito.withSettings().defaultAnswer(Mockito.CALLS_REAL_METHODS))) {
            (new TenantFilter(Collections.emptyList())).doFilterInternal(request, response, filterChain);
            tenantContextHolder.verify(
                    () -> TenantContextHolder.setCurrentTenantCode("tenant2"), times(1));
        }

    }

}
