package org.entando.kubernetes.service.digitalexchange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.apache.commons.lang.StringUtils;
import org.entando.kubernetes.model.bundle.ComponentType;
import org.entando.kubernetes.model.bundle.descriptor.widget.WidgetDescriptor;
import org.entando.kubernetes.model.job.ComponentDataEntity;
import org.entando.kubernetes.model.job.ComponentWidgetData;
import org.entando.kubernetes.model.web.request.Filter;
import org.entando.kubernetes.model.web.request.FilterOperator;
import org.entando.kubernetes.model.web.request.PagedListRequest;
import org.entando.kubernetes.model.web.response.PagedMetadata;
import org.entando.kubernetes.repository.ComponentDataRepository;
import org.entando.kubernetes.repository.InstalledEntandoBundleRepository;
import org.entando.kubernetes.service.digitalexchange.component.ComponentWidgetDataListProcessor;
import org.entando.kubernetes.service.digitalexchange.component.EntandoBundleWidgetService;
import org.entando.kubernetes.service.digitalexchange.component.EntandoBundleWidgetServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.testcontainers.shaded.com.fasterxml.jackson.core.JsonProcessingException;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;
import org.zalando.problem.DefaultProblem;

@Tag("unit")
class EntandoBundleWidgetServiceTest {

    private EntandoBundleWidgetService targetService;
    private ComponentDataRepository componentDataRepository;

    private InstalledEntandoBundleRepository installedEntandoBundleRepository;

    private static final String PREFIX_WIDGET_NAME = "my-widget-name";
    private static final String PREFIX_PLUGIN_NAME = "my-plugin-name";
    private static final String PREFIX_GROUP = "group";
    private static final String PREFIX_BUNDLE_REPO_URL = "http://test.com/gitrepo/reponame";
    private static final int INSTALLED_WIDGET_SIZE = 6;
    private static final String APP_BUILDER_OBJ = "{\"slot\":\"content\"}";
    private static final String COMPONENT_DESCRIPTOR =
            "{\"ext\":{\"appBuilder\":" + APP_BUILDER_OBJ + ", \"adminConsole\":{\"access\":12}}}";

    @BeforeEach
    public void setup() {
        componentDataRepository = Mockito.mock(ComponentDataRepository.class);
        targetService = new EntandoBundleWidgetServiceImpl(componentDataRepository);
    }

    @AfterEach
    public void teardown() {
    }

    @Test
    void listWidgets_shouldReturnBundleListWithCorrectMarshalling() throws JsonProcessingException {
        PagedListRequest req = new PagedListRequest();
        // test all installed
        List<ComponentDataEntity> list = generateWidgetListOf_SIZE();
        when(componentDataRepository.findAll()).thenReturn(list);
        List<ComponentWidgetData> listToTest = targetService.listWidgets(req).getBody().stream()
                .sorted(Comparator.comparing(ComponentWidgetData::getWidgetName))
                .collect(Collectors.toList());
        assertThat(listToTest).hasSize(INSTALLED_WIDGET_SIZE - 1);
        ComponentWidgetData toTest = listToTest.get(0);
        assertThat(toTest.getDescriptorExt()).isEqualTo(APP_BUILDER_OBJ);
        ObjectMapper mapper = new ObjectMapper();
        String value = mapper.writeValueAsString(toTest);
        assertThat(value).contains(APP_BUILDER_OBJ);

        assertThat(listToTest.get(1).getDescriptorExt()).isNull();

        ComponentDataEntity entity = createWidget(1);
        entity.setComponentDescriptor(null);
        when(componentDataRepository.findAll()).thenReturn(new ArrayList<>(Collections.singletonList(entity)));
        assertThrows(DefaultProblem.class, () -> targetService.listWidgets(req));

    }

    @Test
    void getBundles_shouldReturnBundleListFiltered() {
        List<ComponentDataEntity> list = generateWidgetListOf_SIZE();
        when(componentDataRepository.findAll()).thenReturn(list);

        // test filter by bundle id
        PagedListRequest req = new PagedListRequest();
        String bundleIdToSearch = StringUtils.leftPad("21", 8, "0");
        Filter filter = new Filter(ComponentWidgetDataListProcessor.BUNDLE_ID, bundleIdToSearch);
        filter.setOperator(FilterOperator.EQUAL.getValue());
        req.setFilters(new Filter[]{filter});

        assertThat(targetService.listWidgets(req).getBody()).hasSize(1);

        // test filter by widget type
        req = new PagedListRequest();
        filter = new Filter(ComponentWidgetDataListProcessor.WIDGET_TYPE, WidgetDescriptor.TYPE_WIDGET_CONFIG);
        filter.setOperator(FilterOperator.EQUAL.getValue());
        req.setFilters(new Filter[]{filter});
        assertThat(targetService.listWidgets(req).getBody()).hasSize(2);

        // test filter by group
        req = new PagedListRequest();
        filter = new Filter(ComponentWidgetDataListProcessor.BUNDLE_GROUP, PREFIX_GROUP + "1");
        filter.setOperator(FilterOperator.EQUAL.getValue());
        req.setFilters(new Filter[]{filter});
        assertThat(targetService.listWidgets(req).getBody()).hasSize(1);

    }

    //
    @Test
    void getBundles_shouldReturnBundleListSorted() {
        List<ComponentDataEntity> list = generateWidgetListOf_SIZE();
        when(componentDataRepository.findAll()).thenReturn(list);

        // test sort by bundle id desc
        PagedListRequest req = new PagedListRequest();
        req.setSort(ComponentWidgetDataListProcessor.BUNDLE_ID);
        req.setDirection(Filter.DESC_ORDER);

        PagedMetadata<ComponentWidgetData> result = targetService.listWidgets(req);

        assertThat(result.getBody()).hasSize(INSTALLED_WIDGET_SIZE - 1);
        String bundleNameToTest = result.getBody().get(INSTALLED_WIDGET_SIZE - 2).getWidgetName();
        assertThat(bundleNameToTest).isEqualTo(PREFIX_WIDGET_NAME + "1");

        // test sort by bundle id desc
        req = new PagedListRequest();
        req.setSort(ComponentWidgetDataListProcessor.WIDGET_TYPE);
        req.setDirection(Filter.ASC_ORDER);

        result = targetService.listWidgets(req);
        assertThat(result.getBody()).hasSize(INSTALLED_WIDGET_SIZE - 1);
        String widgetTypeToTest = result.getBody().get(INSTALLED_WIDGET_SIZE - 2).getWidgetType();
        assertThat(widgetTypeToTest).isEqualTo(WidgetDescriptor.TYPE_WIDGET_CONFIG);

        // test sort by pub url desc
        req = new PagedListRequest();
        req.setSort(ComponentWidgetDataListProcessor.BUNDLE_GROUP);
        req.setDirection(Filter.DESC_ORDER);

        result = targetService.listWidgets(req);

        assertThat(result.getBody()).hasSize(INSTALLED_WIDGET_SIZE - 1);
        String bundleGroupToTest = result.getBody().get(INSTALLED_WIDGET_SIZE - 2).getBundleGroup();
        assertThat(bundleGroupToTest).isEqualTo(PREFIX_GROUP + "1");

    }

    private List<ComponentDataEntity> generateWidgetListOf_SIZE() {
        List<ComponentDataEntity> list = new ArrayList<>();
        IntStream.range(1, INSTALLED_WIDGET_SIZE + 1).forEach(idx -> {
            String repoUrl = PREFIX_BUNDLE_REPO_URL + idx;
            String bundleId = BundleUtilities.removeProtocolAndGetBundleId(repoUrl);

            ComponentDataEntity entity = null;
            if (idx % INSTALLED_WIDGET_SIZE == 0) {
                entity = createPlugin(idx);
            } else {
                entity = createWidget(idx);
            }
            list.add(entity);
        });
        return list;
    }

    private ComponentDataEntity createPlugin(int idx) {
        String bundleId = StringUtils.leftPad("2" + idx, 8, "0");
        String pluginId = StringUtils.leftPad("" + idx, 8, "0");
        String pluginName = PREFIX_PLUGIN_NAME + idx;
        String pluginCode = "pn-" + bundleId + pluginId + pluginName;
        return ComponentDataEntity.builder().id(UUID.randomUUID()).bundleId(bundleId)
                .componentId(pluginCode)
                .componentName(pluginName).componentCode(pluginCode)
                .componentType(ComponentType.PLUGIN)
                .componentGroup(PREFIX_GROUP + idx)
                .componentDescriptor("{}")
                .build();
    }

    private ComponentDataEntity createWidget(int idx) {
        String bundleId = StringUtils.leftPad("2" + idx, 8, "0");
        String widgetName = PREFIX_WIDGET_NAME + idx;
        String widgetCode = widgetName + "-" + bundleId;
        String componentDescriptor =
                idx != 2 ? "{\"ext\": {\"appBuilder\":" + APP_BUILDER_OBJ + ", \"adminConsole\": {\"access\": 12 }}}"
                        : "{}";
        return ComponentDataEntity.builder().id(UUID.randomUUID()).bundleId(bundleId)
                .componentName(widgetName).componentCode(widgetCode)
                .componentType(ComponentType.WIDGET)
                .componentSubType(selectWidgetSubType(idx))
                .componentGroup(PREFIX_GROUP + idx)
                .componentDescriptor(componentDescriptor)
                .build();
    }

    private String selectWidgetSubType(int idx) {
        switch (idx % 3) {
            case 1:
                return WidgetDescriptor.TYPE_WIDGET_STANDARD;
            case 2:
                return WidgetDescriptor.TYPE_WIDGET_CONFIG;
            default:
                return WidgetDescriptor.TYPE_WIDGET_APPBUILDER;
        }
    }


}