package org.entando.kubernetes.assertionhelper;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertNotNull;

import java.util.List;
import org.assertj.core.api.Java6Assertions;
import org.entando.kubernetes.client.hub.ProxiedPayload;
import org.entando.kubernetes.client.hub.domain.BundleDto;
import org.entando.kubernetes.client.hub.domain.BundleEntityDto;
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

    public static void assertOnBundleGroupVersionsPagedContent(
            PagedContent<BundleGroupVersionFilteredResponseView, BundleGroupVersionEntityDto> result) {
        assertNotNull(result);
        assertThat(result, instanceOf(PagedContent.class));
        PagedContent pc = result;
        assertNotNull(pc.getMetadata());
        assertNotNull(pc.getPayload());
        assertThat(pc.getPayload(), instanceOf(List.class));

        Object elem = pc.getPayload().get(0);
        assertThat(elem, instanceOf(BundleGroupVersionFilteredResponseView.class));
        BundleGroupVersionFilteredResponseView bgv = (BundleGroupVersionFilteredResponseView) elem;
        assertThat(bgv.getBundleGroupId(), equalTo((long) 10));
        assertThat(bgv.getBundleGroupVersionId(), equalTo((long) 100));
        assertThat(bgv.getDocumentationUrl(), equalTo("http://www.entando.com"));
        assertThat(bgv.isPublicCatalog(), equalTo(true));
    }


    public static void assertOnBundlePagedContent(PagedContent<BundleDto, BundleEntityDto> result) {
        assertNotNull(result);
        assertThat(result, instanceOf(PagedContent.class));

        PagedContent pc = result;
        assertNotNull(pc.getMetadata());
        assertNotNull(pc.getPayload());
        assertThat(pc.getPayload(), instanceOf(List.class));

        Object elem = pc.getPayload().get(0);
        assertThat(elem, instanceOf(BundleDto.class));
        BundleDto bgv = (BundleDto) elem;
        assertThat(bgv.getBundleId(), equalTo("2677"));
        assertThat(bgv.getName(), equalTo("Yet another bundle"));
        assertThat(bgv.getDescription(), equalTo("my descr"));
        assertThat(bgv.getGitRepoAddress(), equalTo("http://www.github.com/entando/test"));
        assertThat(bgv.getGitSrcRepoAddress(), equalTo("http://www.github.com/entando/srctest"));
    }

}
