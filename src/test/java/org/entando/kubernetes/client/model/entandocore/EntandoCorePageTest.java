package org.entando.kubernetes.client.model.entandocore;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.entando.kubernetes.model.bundle.descriptor.PageDescriptor;
import org.entando.kubernetes.model.entandocore.EntandoCorePage;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
public class EntandoCorePageTest {

    @Test
    public void shouldReadDescriptorFile() {
       EntandoCorePage ecp = new EntandoCorePage(getTestPageDescriptor());
       assertThat(ecp.getCharset()).isEqualTo("iso1923-12");
       assertThat(ecp.getCode()).isEqualTo("my-page");
       assertThat(ecp.getParentCode()).isEqualTo("plugins");
       assertThat(ecp.isDisplayedInMenu()).isEqualTo(true);
       assertThat(ecp.isSeo()).isEqualTo(false);
       assertThat(ecp.getOwnerGroup()).isEqualTo("administrators");
       assertThat(ecp.getJoinGroups()).containsExactly("free");
       assertThat(ecp.getStatus()).isEqualTo("published");
       assertThat(ecp.getPageModel()).isEqualTo("service");
       assertThat(ecp.getTitles().keySet()).containsExactlyInAnyOrder("it", "en");
       assertThat(ecp.getTitles().values()).containsExactlyInAnyOrder("La mia pagina", "My page");
    }

    @Test
    public void shouldUseDefaultsIfMissingField() {
        PageDescriptor pd = getTestPageDescriptor();
        pd.setPageModel("     ");
        pd.setCharset("");
        pd.setOwnerGroup(null);
        pd.setParentCode("");
        pd.setStatus(null);

        EntandoCorePage ecp = new EntandoCorePage(pd);
        assertThat(ecp.getPageModel()).isEqualTo("home");
        assertThat(ecp.getStatus()).isEqualTo("draft");
        assertThat(ecp.getParentCode()).isEqualTo("homepage");
        assertThat(ecp.getOwnerGroup()).isEqualTo("free");
        assertThat(ecp.getCharset()).isEqualTo("utf-8");
    }

    private PageDescriptor getTestPageDescriptor() {
        Map<String, String> pageTitles = new HashMap<>();
        pageTitles.put("it", "La mia pagina");
        pageTitles.put("en", "My page");

        PageDescriptor pageDescriptor = new PageDescriptor();
        pageDescriptor.setCode("my-page");
        pageDescriptor.setParentCode("plugins");
        pageDescriptor.setCharset("iso1923-12");
        pageDescriptor.setDisplayedInMenu(true);
        pageDescriptor.setPageModel("service");
        pageDescriptor.setOwnerGroup("administrators");
        pageDescriptor.setTitles(pageTitles);
        pageDescriptor.setStatus("published");
        pageDescriptor.setJoinGroups(Collections.singletonList("free"));

        return pageDescriptor;
    }
}
