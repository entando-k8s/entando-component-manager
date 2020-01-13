package org.entando.kubernetes.controller;

import static org.entando.kubernetes.client.k8ssvc.K8SServiceClient.DEFAULT_BUNDLE_NAMESPACE;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.hasValue;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import io.fabric8.kubernetes.client.KubernetesClient;
import java.util.Collections;
import java.util.Date;
import org.entando.kubernetes.client.k8ssvc.K8SServiceClient;
import org.entando.kubernetes.controller.digitalexchange.component.DigitalExchangeComponent;
import org.entando.kubernetes.model.debundle.EntandoDeBundle;
import org.entando.kubernetes.model.debundle.EntandoDeBundleBuilder;
import org.entando.kubernetes.model.debundle.EntandoDeBundleDetails;
import org.entando.kubernetes.model.debundle.EntandoDeBundleSpec;
import org.entando.kubernetes.model.debundle.EntandoDeBundleSpecBuilder;
import org.entando.kubernetes.service.digitalexchange.component.DigitalExchangeComponentsService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
public class DigitalExchangeComponentsTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DigitalExchangeComponentsService componentsService;


    @Test
    public void shouldStart() {

    }

    @Test
    public void apiShouldMaintainCompatibilityWithAppBuilder() throws Exception {

        when(componentsService.getComponents()).thenReturn(Collections.singletonList(convertBundleToLegacyComponent(getTestBundle())));

        mockMvc.perform(get("/components").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("payload", hasSize(1)))
                .andExpect(jsonPath("payload[0]").isMap())
                .andExpect(jsonPath("payload[0]", hasKey("id")))
                .andExpect(jsonPath("payload[0]", hasKey("name")))
                .andExpect(jsonPath("payload[0]", hasKey("lastUpdate")))
                .andExpect(jsonPath("payload[0]", hasKey("version")))
                .andExpect(jsonPath("payload[0]", hasKey("type")))
                .andExpect(jsonPath("payload[0]", hasKey("description")))
                .andExpect(jsonPath("payload[0]", hasKey("image")))
                .andExpect(jsonPath("payload[0]", hasKey("rating")))
                .andExpect(jsonPath("payload[0]", hasKey("digitalExchangeId")))
                .andExpect(jsonPath("payload[0]", hasKey("digitalExchangeName")))
                .andExpect(jsonPath("payload[0].digitalExchangeId").value(DEFAULT_BUNDLE_NAMESPACE));

    }



    private EntandoDeBundle getTestBundle() {
        return new EntandoDeBundleBuilder()
                .withNewMetadata()
                .withName("my-bundle")
                .withNamespace(DEFAULT_BUNDLE_NAMESPACE)
                .endMetadata()
                .withSpec(getTestEntandoDeBundleSpec()).build();

    }

    private EntandoDeBundleSpec getTestEntandoDeBundleSpec() {
        return new EntandoDeBundleSpecBuilder()
                .withNewDetails()
                .withDescription("A bundle containing some demo components for Entando6")
                .withName("inail_bundle")
                .addNewVersion("0.0.1")
                .addNewKeyword("entando6")
                .addNewKeyword("digital-exchange")
                .addNewDistTag("latest", "0.0.1")
                .and()
                .addNewTag()
                .withVersion("0.0.1")
                .withIntegrity("sha512-n4TEroSqg/sZlEGg2xj6RKNtl/t3ZROYdNd99/dl3UrzCUHvBrBxZ1rxQg/sl3kmIYgn3+ogbIFmUZYKWxG3Ag==")
                .withShasum("4d80130d7d651176953b5ce470c3a6f297a70815")
                .withTarball("http://localhost:8081/repository/npm-internal/inail_bundle/-/inail_bundle-0.0.1.tgz")
                .endTag()
                .build();
    }


    private DigitalExchangeComponent convertBundleToLegacyComponent(EntandoDeBundle bundle) {
        DigitalExchangeComponent dec = new DigitalExchangeComponent();
        EntandoDeBundleDetails bd = bundle.getSpec().getDetails();
        dec.setDescription(bd.getDescription());
        dec.setDigitalExchangeId(DEFAULT_BUNDLE_NAMESPACE);
        dec.setDigitalExchangeName("Default-digital-exchange");
        dec.setId(bd.getName());
        dec.setRating(5);
        dec.setInstalled(false);
        dec.setType("Bundle");
        dec.setLastUpdate(new Date());
        dec.setSignature("");
        dec.setVersion(bd.getDistTags().get("latest").toString());
        return dec;
    }

}
