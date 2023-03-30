package org.entando.kubernetes.assertionhelper;

import org.assertj.core.api.Java6Assertions;
import org.entando.kubernetes.client.hub.ProxiedPayload;
import org.entando.kubernetes.client.hub.domain.BundleGroupVersionEntityDto;
import org.entando.kubernetes.client.hub.domain.BundleGroupVersionFilteredResponseView;
import org.entando.kubernetes.client.hub.domain.PagedContent;
import org.springframework.http.HttpStatus;

public class HubAssertionHelper {

    public static void assertOnSuccessfulProxiedPayload(ProxiedPayload<PagedContent<?, ?>> proxiedPayload) {
        Java6Assertions.assertThat(proxiedPayload.getStatus()).isEqualTo(HttpStatus.OK);
        Java6Assertions.assertThat(proxiedPayload.getExceptionClass()).isNullOrEmpty();
        Java6Assertions.assertThat(proxiedPayload.getExceptionMessage()).isNullOrEmpty();
        Java6Assertions.assertThat(proxiedPayload.getPayload()).isNotNull();
        Java6Assertions.assertThat(proxiedPayload.getPayload().getPayload().size()).isGreaterThan(0);
    }

    public static void assertOnFailingProxiedPayload(ProxiedPayload<PagedContent<?, ?>> proxiedPayload) {
        Java6Assertions.assertThat(proxiedPayload.getStatus()).isNotEqualTo(HttpStatus.OK);
        Java6Assertions.assertThat(proxiedPayload.getStatus()).isNotEqualTo(HttpStatus.ACCEPTED);
        Java6Assertions.assertThat(proxiedPayload.getExceptionClass()).isNotEmpty();
        Java6Assertions.assertThat(proxiedPayload.getExceptionMessage()).isNotEmpty();
        Java6Assertions.assertThat(proxiedPayload.getPayload()).isNull();
    }

    public static void assertOnBundleGroupVersionsPagedContent(PagedContent<BundleGroupVersionFilteredResponseView, BundleGroupVersionEntityDto> pagedContent) {
        // TODO validation stuff
    }
}
