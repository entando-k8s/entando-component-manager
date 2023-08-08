package org.entando.kubernetes.controller.mockmvc;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import org.entando.kubernetes.EntandoKubernetesJavaApplication;
import org.entando.kubernetes.assertionhelper.EntandoHubRegistryAssertionHelper;
import org.entando.kubernetes.config.TestAppConfiguration;
import org.entando.kubernetes.config.TestKubernetesConfig;
import org.entando.kubernetes.config.TestSecurityConfiguration;
import org.entando.kubernetes.config.TestTenantConfiguration;
import org.entando.kubernetes.model.entandohub.EntandoHubRegistry;
import org.entando.kubernetes.model.entandohub.EntandoHubRegistryEntity;
import org.entando.kubernetes.repository.EntandoHubRegistryRepository;
import org.entando.kubernetes.security.AuthorizationChecker;
import org.entando.kubernetes.stubhelper.EntandoHubRegistryStubHelper;
import org.entando.kubernetes.utils.TestInstallUtils;
import org.hamcrest.core.IsNull;
import org.hamcrest.text.IsEmptyString;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import wiremock.com.jayway.jsonpath.JsonPath;

@AutoConfigureWireMock(port = 8103)
@AutoConfigureMockMvc
@SpringBootTest(
        webEnvironment = WebEnvironment.RANDOM_PORT,
        classes = {
                EntandoKubernetesJavaApplication.class,
                TestSecurityConfiguration.class,
                TestKubernetesConfig.class,
                TestAppConfiguration.class,
                TestTenantConfiguration.class
        })
@ActiveProfiles({"test"})
@Tag("component")
@WithMockUser
@DirtiesContext
class EntandoHubRegistryIntegrationTest {

    private final String baseUrl = "/registries";

    private MockMvc mockMvc;
    private List<EntandoHubRegistryEntity> entityToSaveList = EntandoHubRegistryStubHelper.stubListOfEntandoHubRegistryEntity();
    private List<EntandoHubRegistry> savedRegistryList = EntandoHubRegistryStubHelper.stubListOfEntandoHubRegistry();
    private ObjectMapper mapper = new ObjectMapper();

    @Autowired
    private WebApplicationContext context;
    @Autowired
    private EntandoHubRegistryRepository entandoHubRegistryRepository;
    @Autowired
    private AuthorizationChecker authorizationChecker;

    @BeforeEach
    public void setup() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
        entandoHubRegistryRepository.saveAll(entityToSaveList);

        TestInstallUtils.injectEntandoUrlInto(authorizationChecker, 8103);
        TestInstallUtils.stubPermissionRequestReturningSuperuser();
    }

    @AfterEach
    public void tearDown() {
        entandoHubRegistryRepository.deleteAll();
    }

    @Test
    void shouldReturnTheExpectedListOfEntandoHubRegistry() throws Exception {

        // given that 2 registries are present in the db
        // when the user asks for the list of registries
        // then the list of registries is correctly returned
        getAndValidateRegistryListWithTheTwoStartingRegistries();
    }

    @Test
    void shouldReturnErrorWhenAddingANewRegistryWithAnAlreadyPopulatedId() throws Exception {

        // given that the user sends a registry to add with a pre populated id
        final EntandoHubRegistry registryToAdd = EntandoHubRegistryStubHelper.stubEntandoHubRegistry3();

        executeFailingPostOrPut(MockMvcRequestBuilders::post, registryToAdd,
                "The received Entando Hub registry has a populated ID and it needs an empty one");
    }

    @Test
    void shouldReturnErrorWhenAddingANewRegistryWithAnExistingNameOrUrl() throws Exception {

        // given that the user wants to add a registry with an existing name
        EntandoHubRegistry registryWithExistingName = EntandoHubRegistryStubHelper.stubEntandoHubRegistry3()
                .setId(null)
                .setName(EntandoHubRegistryStubHelper.REGISTRY_NAME_1);

        executeFailingPostOrPut(MockMvcRequestBuilders::post, registryWithExistingName,
                "An Entando Hub registry with this name is already present", status().is4xxClientError());

        // given that the user wants to add a registry with an existing name
        EntandoHubRegistry registryWithExistingUrl = EntandoHubRegistryStubHelper.stubEntandoHubRegistry3()
                .setId(null)
                .setUrl(EntandoHubRegistryStubHelper.REGISTRY_URL_STRING_1);

        executeFailingPostOrPut(MockMvcRequestBuilders::post, registryWithExistingUrl,
                "An Entando Hub registry with this url is already present", status().is4xxClientError());
    }

    @Test
    void shouldAddANewRegistryAndReturnItInTheResponseAndReturnItInTheListOfRegistries() throws Exception {

        // given that 2 registries are present in the db
        getAndValidateRegistryListWithTheTwoStartingRegistries();

        // and that the user sends a correctly populated registry to add
        EntandoHubRegistry registryToAdd = EntandoHubRegistryStubHelper.stubEntandoHubRegistry3();
        registryToAdd.setId(null);

        // when the user sends the request
        final ResultActions resultAdd = mockMvc.perform(
                post(baseUrl)
                        .header(HttpHeaders.AUTHORIZATION, "jwt")
                        .content(mapper.writeValueAsString(registryToAdd))
                        .contentType(MediaType.APPLICATION_JSON_VALUE));

        // then a successful response is returned
        resultAdd.andExpect(status().isCreated());
        EntandoHubRegistryAssertionHelper.assertOnEntandoHubRegistryEntity(resultAdd, null, registryToAdd);
        assertOnSuccessfulSimpleRestResponse(resultAdd);

        // and an updated list of registries is returned
        ResultActions resultList = mockMvc.perform(get(baseUrl));
        EntandoHubRegistryAssertionHelper.assertOnEntandoHubRegistryEntityList(resultList,
                Arrays.asList(savedRegistryList.get(0), savedRegistryList.get(1), registryToAdd));
        assertOnSuccessfulSimpleRestResponse(resultList);
    }

    @Test
    void shouldReturnErrorWhenUpdatingANonExistingRegistry() throws Exception {

        // given that the user sends a registry to update that doesn't exist
        EntandoHubRegistry registryToUpdate = EntandoHubRegistryStubHelper.stubEntandoHubRegistry3();
        registryToUpdate.setId(UUID.randomUUID().toString());

        executeFailingPostOrPut(MockMvcRequestBuilders::put, registryToUpdate,
                "No registry found for the received ID", status().is5xxServerError());
    }

    @Test
    void shouldReturnErrorWhenUpdatingAnExistingRegistryWithoutId() throws Exception {

        // given that the user sends a registry to add with a pre populated id
        EntandoHubRegistry registryToUpdate = EntandoHubRegistryStubHelper.stubEntandoHubRegistry3();
        registryToUpdate.setId(null);

        executeFailingPostOrPut(MockMvcRequestBuilders::put, registryToUpdate,
                "The received Entando Hub registry has an empty ID and it needs a populated one");
    }

    @Test
    void shouldReturnErrorWhenUpdatingARegistryWithAnExistingNameOrUrlBelongingToAnotherRegistry() throws Exception {

        // given that the user wants to add a registry with an existing name
        EntandoHubRegistry registryWithExistingName = EntandoHubRegistryStubHelper.stubEntandoHubRegistry3()
                .setName(EntandoHubRegistryStubHelper.REGISTRY_NAME_1);

        executeFailingPostOrPut(MockMvcRequestBuilders::put, registryWithExistingName,
                "An Entando Hub registry with this name is already present", status().is4xxClientError());

        // given that the user wants to add a registry with an existing name
        EntandoHubRegistry registryWithExistingUrl = EntandoHubRegistryStubHelper.stubEntandoHubRegistry3()
                .setUrl(EntandoHubRegistryStubHelper.REGISTRY_URL_STRING_1);

        executeFailingPostOrPut(MockMvcRequestBuilders::put, registryWithExistingUrl,
                "An Entando Hub registry with this url is already present", status().is4xxClientError());

    }

    @Test
    void shouldUpdateARegistryAndReturnItInTheResponseAndReturnItInTheListOfRegistries() throws Exception {

        String newName = "name_4";
        String newUrl = "http://www.entando.com/new_registry";

        // given that 2 registries are present in the db
        ResultActions resultList = getAndValidateRegistryListWithTheTwoStartingRegistries();

        // when that the user sends a correctly populated registry to update
        String response = resultList.andReturn().getResponse().getContentAsString();
        EntandoHubRegistry registryToUpdate = new EntandoHubRegistry()
                .setId(JsonPath.parse(response).read("$.payload.[0].id").toString())
                .setName(newName)
                .setUrl(newUrl);
        final ResultActions resultUpdate = mockMvc.perform(
                put(baseUrl)
                        .header(HttpHeaders.AUTHORIZATION, "jwt")
                        .content(mapper.writeValueAsString(registryToUpdate))
                        .contentType(MediaType.APPLICATION_JSON_VALUE));

        // then a successful response is returned
        resultUpdate.andExpect(status().isOk());
        EntandoHubRegistryAssertionHelper.assertOnEntandoHubRegistryEntity(resultUpdate, null, registryToUpdate);
        assertOnSuccessfulSimpleRestResponse(resultUpdate);

        // and an updated list of registries is returned
        resultList = mockMvc.perform(get(baseUrl));
        EntandoHubRegistryAssertionHelper.assertOnEntandoHubRegistryEntityList(resultList,
                Arrays.asList(savedRegistryList.get(1), registryToUpdate));
        assertOnSuccessfulSimpleRestResponse(resultList);
    }


    @Test
    void shouldDeleteAnExistingRegistryAndReturnItsName() throws Exception {

        // given that 2 registries are present in the db
        ResultActions resultList = getAndValidateRegistryListWithTheTwoStartingRegistries();

        // when the user sends a request to delete one of those registries
        String response = resultList.andReturn().getResponse().getContentAsString();
        final String idToDelete = JsonPath.parse(response).read("$.payload.[0].id").toString();
        final String nameToDelete = JsonPath.parse(response).read("$.payload.[0].name").toString();

        // then a successful response is returned
        final ResultActions resultDelete = mockMvc.perform(delete(baseUrl + "/" + idToDelete)
                .header(HttpHeaders.AUTHORIZATION, "jwt"));
        resultDelete
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payload.name", is(nameToDelete)));

        // and the new present list of registries does contain only one record
        resultList = mockMvc.perform(get(baseUrl));
        EntandoHubRegistryAssertionHelper.assertOnEntandoHubRegistryEntityList(resultList,
                List.of(savedRegistryList.get(1)));
    }

    @Test
    void shouldReturnEmptyStringIfRegistryToDeleteHasNotBeenFound() throws Exception {

        // given that 2 registries are present in the db
        ResultActions resultList = getAndValidateRegistryListWithTheTwoStartingRegistries();

        // when the user sends a request to delete a registry not present
        // then a successful response is returned
        final ResultActions resultDelete = mockMvc.perform(delete(baseUrl + "/" + UUID.randomUUID())
                .header(HttpHeaders.AUTHORIZATION, "jwt"));
        resultDelete
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payload.name", IsEmptyString.emptyString()));
    }


    private ResultActions getAndValidateRegistryListWithTheTwoStartingRegistries() throws Exception {
        ResultActions resultList = mockMvc.perform(get(baseUrl));
        resultList.andExpect(jsonPath("$.payload", hasSize(2)));
        EntandoHubRegistryAssertionHelper.assertOnEntandoHubRegistryEntityList(resultList,
                Arrays.asList(savedRegistryList.get(0), savedRegistryList.get(1)));
        assertOnSuccessfulSimpleRestResponse(resultList);
        return resultList;
    }

    private void assertOnSuccessfulSimpleRestResponse(ResultActions result) throws Exception {

        result.andExpect(jsonPath("metaData").value(IsNull.nullValue()))
                .andExpect(jsonPath("errors").isArray())
                .andExpect(jsonPath("errors", hasSize(0)));
    }


    /**
     * execute a failing post or put against the baseUrl, sending a registry and asserting that a 500 is returned with
     * the proper error.
     *
     * @param httpMethodFn the post or put http method do execute
     * @param registry     the registry to send in the body
     * @param error        the expected error
     */
    private void executeFailingPostOrPut(Function<String, MockHttpServletRequestBuilder> httpMethodFn,
            EntandoHubRegistry registry,
            String error) throws Exception {

        executeFailingPostOrPut(httpMethodFn, registry, error, status().is4xxClientError());
    }

    /**
     * execute a failing post or put against the baseUrl, sending a registry and asserting that a 500 is returned with
     * the proper error.
     *
     * @param httpMethodFn         the post or put http method do execute
     * @param registry             the registry to send in the body
     * @param error                the expected error
     * @param httpStatusResMatched the epected status code ResultMatcherˇ
     */
    private void executeFailingPostOrPut(Function<String, MockHttpServletRequestBuilder> httpMethodFn,
            EntandoHubRegistry registry,
            String error,
            ResultMatcher httpStatusResMatched) throws Exception {

        // when the user sends the request
        ResultActions result = mockMvc.perform(
                httpMethodFn.apply(baseUrl)
                        .content(mapper.writeValueAsString(registry))
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .header(HttpHeaders.AUTHORIZATION, "jwt"));

        // then an error is returned
        result.andExpect(httpStatusResMatched)
                .andExpect(jsonPath("message",
                        is(error)));
    }
}
