package org.entando.kubernetes.client;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.util.List;
import org.entando.kubernetes.model.app.EntandoApp;
import org.entando.kubernetes.model.plugin.EntandoPlugin;
import org.junit.Ignore;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.client.Hop;
import org.springframework.hateoas.client.Traverson;

public class TraversonTest {

    @Test
    @Disabled("Scope of this is to evaluate functionalities of Traverson on a real service, not part of tests")
    public void testTraversonGetCollection() {
       Traverson traverson = new Traverson(URI.create("http://qst-eci-cm-cui.lab.entando.org/k8s/"), MediaTypes.HAL_JSON);
       EntityModel<EntandoApp> app = traverson.follow("apps")
               .follow(Hop.rel("app").withParameter("name", "qst"))
               .toObject(new ParameterizedTypeReference<EntityModel<EntandoApp>>() {});
       EntityModel<EntandoPlugin> plugin = traverson.follow("plugins")
               .follow(Hop.rel("plugin").withParameter("name", "tecnici-plugin"))
               .toObject(new ParameterizedTypeReference<EntityModel<EntandoPlugin>>() { });
       assertThat(app.getContent().getMetadata().getName()).isEqualTo("qst");
       assertThat(plugin.getContent().getMetadata().getName()).isEqualTo("tecnici-plugin");

    }

    @Test
    @Disabled("Scope of this is to evaluate functionalities of Traverson on a real service, not part of tests")
    public void testTraversonJsonPath() {
        Traverson traverson = new Traverson(URI.create("http://qst-eci-cm-cui.lab.entando.org/k8s/"), MediaTypes.HAL_JSON);
        List<String> appNames = traverson.follow("apps")
                .toObject("$._embedded.entandoApps.*.metadata.name");
        assertThat(appNames).containsOnly("qst");
    }

}
