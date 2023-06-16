package org.entando.kubernetes.client.core;

import java.io.File;
import java.net.URI;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;
import org.entando.kubernetes.client.model.AnalysisReport;
import org.entando.kubernetes.client.request.AnalysisReportClientRequest;
import org.entando.kubernetes.client.request.AnalysisReportClientRequestFactory;
import org.entando.kubernetes.config.tenant.TenantRestTemplateAccessor;
import org.entando.kubernetes.exception.digitalexchange.ReportAnalysisException;
import org.entando.kubernetes.exception.web.WebHttpException;
import org.entando.kubernetes.model.bundle.descriptor.AssetDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.CategoryDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.ContentTemplateDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.FileDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.FragmentDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.GroupDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.LabelDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.LanguageDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.PageDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.PageTemplateDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.content.ContentDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.contenttype.ContentTypeDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.widget.WidgetConfigurationDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.widget.WidgetDescriptor;
import org.entando.kubernetes.model.bundle.downloader.MethodRetryer;
import org.entando.kubernetes.model.bundle.reportable.Reportable;
import org.entando.kubernetes.model.bundle.reportable.ReportableRemoteHandler;
import org.entando.kubernetes.model.entandocore.EntandoCoreComponentUsage;
import org.entando.kubernetes.model.entandocore.EntandoCoreContentModel;
import org.entando.kubernetes.model.entandocore.EntandoCoreFile;
import org.entando.kubernetes.model.entandocore.EntandoCoreFolder;
import org.entando.kubernetes.model.entandocore.EntandoCoreFragment;
import org.entando.kubernetes.model.entandocore.EntandoCoreLanguage;
import org.entando.kubernetes.model.entandocore.EntandoCorePage;
import org.entando.kubernetes.model.entandocore.EntandoCorePageTemplate;
import org.entando.kubernetes.model.entandocore.EntandoCorePageWidgetConfiguration;
import org.entando.kubernetes.model.entandocore.EntandoCoreWidget;
import org.entando.kubernetes.model.web.response.RestResponse;
import org.entando.kubernetes.model.web.response.SimpleRestResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@Service
public class DefaultEntandoCoreClient implements EntandoCoreClient {

    private static final String API_PATH_SEGMENT = "api";
    private static final String ANALYSIS_PATH_SEGMENT = "analysis";
    private static final String CMS_PATH_SEGMENT = "cms";
    private static final String PAGES_PATH_SEGMENT = "pages";
    private static final String PAGE_MODELS_PATH_SEGMENT = "pageModels";
    private static final String WIDGETS_PATH_SEGMENT = "widgets";
    private static final String FRAGMENTS_PATH_SEGMENT = "fragments";
    private static final String LABELS_PATH_SEGMENT = "labels";
    private static final String LANGUAGES_PATH_SEGMENT = "languages";
    private static final String GROUPS_PATH_SEGMENT = "groups";
    private static final String PLUGINS_PATH_SEGMENT = "plugins";
    private static final String CONTENT_TYPES_PATH_SEGMENT = "contentTypes";
    private static final String CONTENT_MODELS_PATH_SEGMENT = "contentmodels";
    private static final String CONTENTS_PATH_SEGMENT = "contents";
    private static final String FILE_BROWSER_PATH_SEGMENT = "fileBrowser";
    private static final String CATEGORIES_PATH_SEGMENT = "categories";
    private static final String COMPONENTS_PATH_SEGMENT = "components";
    private static final String USAGE_PATH_SEGMENT = "usage";
    private static final String DIFF_PATH_SEGMENT = "diff";
    private static final String STATUS_PATH_SEGMENT = "status";
    private static final String ASSETS_PATH_SEGMENT = "assets";
    private static final String DIRECTORY_PATH_SEGMENT = "directory";
    private static final String FILE_PATH_SEGMENT = "file";
    
    private final String entandoUrl;
    
    private final TenantRestTemplateAccessor accessor;
    
    private final int retryNumber = parseIntOrDefault("ENTANDO_ECR_DEAPP_REQUEST_RETRIES", 3);
    private final long backOffPeriod = parseIntOrDefault("ENTANDO_ECR_DEAPP_REQUEST_BACKOFF", 5);
    
    private final List<HttpStatus> errorsCandidatesToRetry = Arrays.asList(
            HttpStatus.TOO_MANY_REQUESTS,
            HttpStatus.INTERNAL_SERVER_ERROR, HttpStatus.BAD_GATEWAY,
            HttpStatus.SERVICE_UNAVAILABLE, HttpStatus.GATEWAY_TIMEOUT,
            HttpStatus.INSUFFICIENT_STORAGE, HttpStatus.BANDWIDTH_LIMIT_EXCEEDED
    );
    
    public DefaultEntandoCoreClient(@Value("${entando.url}") String entandoUrl, @Autowired TenantRestTemplateAccessor accessor) {
        this.entandoUrl = entandoUrl;
        this.accessor = accessor;
    }
    
    private RestTemplate getRestTemplate() {
        return this.accessor.getRestTemplate();
    }
    
    @Override
    public void createWidget(final WidgetDescriptor descriptor) {
        MethodRetryer<Runnable, Object> retryer = this.buildDefaultRetryer();
        Runnable r = () -> this.getRestTemplate()
                .postForEntity(resolvePathSegments(API_PATH_SEGMENT, WIDGETS_PATH_SEGMENT).build().toUri(),
                        new EntandoCoreWidget(descriptor),
                        Void.class);
        retryer.execute(r);
    }

    @Override
    public void updateWidget(WidgetDescriptor descriptor) {
        MethodRetryer<Runnable, Object> retryer = this.buildDefaultRetryer();
        Runnable r = () -> this.getRestTemplate()
                .put(resolvePathSegments(API_PATH_SEGMENT, WIDGETS_PATH_SEGMENT, descriptor.getCode()).build()
                                .toUri(),
                        new EntandoCoreWidget(descriptor));
        retryer.execute(r);
    }

    @Override
    public void deleteWidget(final String code) {
        notFoundOrUnauthorizedProtectedDelete(
                resolvePathSegments(API_PATH_SEGMENT, WIDGETS_PATH_SEGMENT, code).build().toUri());
    }

    @Override
    public EntandoCoreComponentUsage getWidgetUsage(String code) {
        return this
                .getComponentUsage(code, new String[]{API_PATH_SEGMENT, WIDGETS_PATH_SEGMENT, code, USAGE_PATH_SEGMENT},
                        "widget");
    }

    @Override
    public void createFragment(FragmentDescriptor descriptor) {
        this.getRestTemplate().postForEntity(resolvePathSegments(API_PATH_SEGMENT, FRAGMENTS_PATH_SEGMENT).build().toUri(),
                new EntandoCoreFragment(descriptor), Void.class);
    }

    @Override
    public void updateFragment(FragmentDescriptor descriptor) {
        this.getRestTemplate().put(resolvePathSegments(API_PATH_SEGMENT, FRAGMENTS_PATH_SEGMENT, descriptor.getCode()).build()
                        .toUri(),
                new EntandoCoreFragment(descriptor));
    }

    @Override
    public void deleteFragment(String code) {
        notFoundOrUnauthorizedProtectedDelete(
                resolvePathSegments(API_PATH_SEGMENT, FRAGMENTS_PATH_SEGMENT, code).build().toUri());
    }

    @Override
    public EntandoCoreComponentUsage getFragmentUsage(String code) {
        return this.getComponentUsage(code,
                new String[]{API_PATH_SEGMENT, FRAGMENTS_PATH_SEGMENT, code, USAGE_PATH_SEGMENT},
                "fragment");
    }

    @Override
    public void createLabel(final LabelDescriptor descriptor) {
        this.getRestTemplate()
                .postForEntity(resolvePathSegments(API_PATH_SEGMENT, LABELS_PATH_SEGMENT).build().toUri(), descriptor,
                        Void.class);
    }

    @Override
    public void updateLabel(LabelDescriptor descriptor) {
        this.getRestTemplate()
                .put(resolvePathSegments(API_PATH_SEGMENT, LABELS_PATH_SEGMENT, descriptor.getKey()).build().toUri(),
                        descriptor);
    }

    @Override
    public void deleteLabel(final String code) {
        notFoundOrUnauthorizedProtectedDelete(
                resolvePathSegments(API_PATH_SEGMENT, LABELS_PATH_SEGMENT, code).build().toUri());
    }

    @Override
    public void enableLanguage(final LanguageDescriptor descriptor) {
        descriptor.setActive(true);
        this.getRestTemplate().put(resolvePathSegments(API_PATH_SEGMENT, LANGUAGES_PATH_SEGMENT, descriptor.getCode()).build()
                        .toUri(),
                new EntandoCoreLanguage(descriptor));
    }

    @Override
    public void disableLanguage(final String code) {
        EntandoCoreLanguage entandoCoreLanguage = new EntandoCoreLanguage()
                .setCode(code)
                .setActive(false);

        try {
            this.getRestTemplate().put(resolvePathSegments(API_PATH_SEGMENT, LANGUAGES_PATH_SEGMENT, code).build().toUri(),
                    entandoCoreLanguage);
        } catch (RestClientResponseException e) {
            HttpStatus s = HttpStatus.resolve(e.getRawStatusCode());
            if (s == null || (!s.is2xxSuccessful() && !s.equals(HttpStatus.CONFLICT))) {
                throw e;
            }
        }
    }


    @Override
    public void createGroup(GroupDescriptor descriptor) {
        this.getRestTemplate()
                .postForEntity(resolvePathSegments(API_PATH_SEGMENT, GROUPS_PATH_SEGMENT).build().toUri(), descriptor,
                        Void.class);
    }

    @Override
    public void updateGroup(GroupDescriptor descriptor) {
        this.getRestTemplate()
                .put(resolvePathSegments(API_PATH_SEGMENT, GROUPS_PATH_SEGMENT, descriptor.getCode()).build().toUri(),
                        descriptor);
    }

    @Override
    public void deleteGroup(String code) {
        notFoundOrUnauthorizedProtectedDelete(
                resolvePathSegments(API_PATH_SEGMENT, GROUPS_PATH_SEGMENT, code).build().toUri());
    }

    @Override
    public EntandoCoreComponentUsage getGroupUsage(String code) {
        return this
                .getComponentUsage(code, new String[]{API_PATH_SEGMENT, GROUPS_PATH_SEGMENT, code, USAGE_PATH_SEGMENT},
                        "group");
    }

    @Override
    public void createPage(PageDescriptor pageDescriptor) {
        this.getRestTemplate()
                .postForEntity(resolvePathSegments(API_PATH_SEGMENT, PAGES_PATH_SEGMENT).build().toUri(),
                        new EntandoCorePage(pageDescriptor),
                        Void.class);
    }

    @Override
    public void updatePageConfiguration(PageDescriptor pageDescriptor) {
        this.getRestTemplate().put(resolvePathSegments(API_PATH_SEGMENT, PAGES_PATH_SEGMENT, pageDescriptor.getCode()).build()
                        .toUri(),
                new EntandoCorePage(pageDescriptor));
    }

    @Override
    public void configurePageWidget(PageDescriptor pageDescriptor, WidgetConfigurationDescriptor widgetDescriptor) {
        this.getRestTemplate().put(resolvePathSegments(API_PATH_SEGMENT, PAGES_PATH_SEGMENT, pageDescriptor.getCode(),
                WIDGETS_PATH_SEGMENT,
                widgetDescriptor.getPos().toString()).build().toUri(),
                new EntandoCorePageWidgetConfiguration(widgetDescriptor));
    }

    @Override
    public void setPageStatus(String code, String status) {
        this.getRestTemplate()
                .put(resolvePathSegments(API_PATH_SEGMENT, PAGES_PATH_SEGMENT, code,
                        STATUS_PATH_SEGMENT).build().toUri(),
                        Collections.singletonMap("status", status));   // NOSONAR
    }

    @Override
    public void deletePage(final String code) {
        notFoundOrUnauthorizedProtectedDelete(
                resolvePathSegments(API_PATH_SEGMENT, PAGES_PATH_SEGMENT, code).build().toUri());
    }

    @Override
    public EntandoCoreComponentUsage getPageUsage(String code) {
        return this
                .getComponentUsage(code, new String[]{API_PATH_SEGMENT, PAGES_PATH_SEGMENT, code, USAGE_PATH_SEGMENT},
                        "page");
    }

    @Override
    public void createPageTemplate(final PageTemplateDescriptor descriptor) {
        this.getRestTemplate().postForEntity(resolvePathSegments(API_PATH_SEGMENT, PAGE_MODELS_PATH_SEGMENT).build().toUri(),
                new EntandoCorePageTemplate(descriptor), Void.class);
    }

    @Override
    public void updatePageTemplate(PageTemplateDescriptor descriptor) {
        this.getRestTemplate().put(resolvePathSegments(API_PATH_SEGMENT, PAGE_MODELS_PATH_SEGMENT, descriptor.getCode()).build()
                        .toUri(),
                new EntandoCorePageTemplate(descriptor));
    }

    @Override
    public void deletePageModel(final String code) {
        notFoundOrUnauthorizedProtectedDelete(
                resolvePathSegments(API_PATH_SEGMENT, PAGE_MODELS_PATH_SEGMENT, code).build().toUri());
    }

    @Override
    public EntandoCoreComponentUsage getPageModelUsage(String code) {
        return this
                .getComponentUsage(code,
                        new String[]{API_PATH_SEGMENT, PAGE_MODELS_PATH_SEGMENT, code, USAGE_PATH_SEGMENT},
                        "page template");
    }

    @Override
    public void deleteContentModel(final String code) {
        notFoundOrUnauthorizedProtectedDelete(
                resolvePathSegments(API_PATH_SEGMENT, PLUGINS_PATH_SEGMENT, CMS_PATH_SEGMENT,
                        CONTENT_MODELS_PATH_SEGMENT, code).build()
                        .toUri());
    }

    @Override
    public void createContentTemplate(final ContentTemplateDescriptor descriptor) {
        this.getRestTemplate().postForEntity(
                resolvePathSegments(API_PATH_SEGMENT, PLUGINS_PATH_SEGMENT, CMS_PATH_SEGMENT,
                        CONTENT_MODELS_PATH_SEGMENT).build().toUri(),
                new EntandoCoreContentModel(descriptor), Void.class);
    }

    @Override
    public void updateContentTemplate(ContentTemplateDescriptor descriptor) {
        this.getRestTemplate().put(resolvePathSegments(API_PATH_SEGMENT, PLUGINS_PATH_SEGMENT, CMS_PATH_SEGMENT,
                CONTENT_MODELS_PATH_SEGMENT,
                descriptor.getId()).build().toUri(), new EntandoCoreContentModel(descriptor));
    }

    @Override
    public EntandoCoreComponentUsage getContentModelUsage(String code) {
        return this.getComponentUsage(code,
                new String[]{API_PATH_SEGMENT, PLUGINS_PATH_SEGMENT, CMS_PATH_SEGMENT,
                        CONTENT_MODELS_PATH_SEGMENT, code, USAGE_PATH_SEGMENT},
                "content template");
    }

    @Override
    public void createContentType(final ContentTypeDescriptor descriptor) {
        this.getRestTemplate()
                .postForEntity(
                        resolvePathSegments(API_PATH_SEGMENT, PLUGINS_PATH_SEGMENT, CMS_PATH_SEGMENT,
                                CONTENT_TYPES_PATH_SEGMENT).build()
                                .toUri(), descriptor,
                        Void.class);
    }

    @Override
    public void updateContentType(ContentTypeDescriptor descriptor) {
        this.getRestTemplate()
                .put(resolvePathSegments(API_PATH_SEGMENT, PLUGINS_PATH_SEGMENT, CMS_PATH_SEGMENT,
                        CONTENT_TYPES_PATH_SEGMENT).build().toUri(),
                        descriptor);
    }

    @Override
    public void deleteContentType(final String code) {
        notFoundOrUnauthorizedProtectedDelete(
                resolvePathSegments(API_PATH_SEGMENT, PLUGINS_PATH_SEGMENT, CMS_PATH_SEGMENT,
                        CONTENT_TYPES_PATH_SEGMENT, code).build()
                        .toUri());
    }

    @Override
    public EntandoCoreComponentUsage getContentTypeUsage(String code) {
        return this
                .getComponentUsage(code,
                        new String[]{API_PATH_SEGMENT, PLUGINS_PATH_SEGMENT, CMS_PATH_SEGMENT,
                                CONTENT_TYPES_PATH_SEGMENT, code,
                                USAGE_PATH_SEGMENT},
                        "content type");
    }

    @Override
    public void createContent(ContentDescriptor descriptor) {
        this.getRestTemplate()
                .postForEntity(
                        resolvePathSegments(API_PATH_SEGMENT, PLUGINS_PATH_SEGMENT, CMS_PATH_SEGMENT,
                                CONTENTS_PATH_SEGMENT).build().toUri(),
                        Collections.singletonList(descriptor),
                        Void.class);
    }

    @Override
    public void updateContent(ContentDescriptor descriptor) {
        this.getRestTemplate()
                .put(resolvePathSegments(API_PATH_SEGMENT, PLUGINS_PATH_SEGMENT, CMS_PATH_SEGMENT,
                        CONTENTS_PATH_SEGMENT, descriptor.getId())
                                .build().toUri(),
                        descriptor);
    }

    @Override
    public void publishContent(ContentDescriptor descriptor) {
        this.getRestTemplate()
                .put(resolvePathSegments(API_PATH_SEGMENT, PLUGINS_PATH_SEGMENT, CMS_PATH_SEGMENT,
                        CONTENTS_PATH_SEGMENT, descriptor.getId(),
                        "status")
                        .build().toUri(), Collections.singletonMap("status", "published"));
    }

    @Override
    public void deleteContent(String code) {
        notFoundOrUnauthorizedProtectedDelete(
                resolvePathSegments(API_PATH_SEGMENT, PLUGINS_PATH_SEGMENT, CMS_PATH_SEGMENT,
                        CONTENTS_PATH_SEGMENT, code).build().toUri());
    }

    @Override
    public void createAsset(AssetDescriptor descriptor, File file) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("metadata", descriptor);
        body.add("file", new FileSystemResource(file));

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        this.getRestTemplate()
                .exchange(resolvePathSegments(API_PATH_SEGMENT, PLUGINS_PATH_SEGMENT, CMS_PATH_SEGMENT,
                        ASSETS_PATH_SEGMENT)
                                .build().toUri(),
                        HttpMethod.POST, requestEntity, String.class);
    }

    @Override
    public void updateAsset(AssetDescriptor descriptor, File file) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("metadata", descriptor);
        body.add("file", new FileSystemResource(file));

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        this.getRestTemplate().exchange(
                resolvePathSegments(API_PATH_SEGMENT, PLUGINS_PATH_SEGMENT, CMS_PATH_SEGMENT,
                        ASSETS_PATH_SEGMENT,
                        "cc=" + descriptor.getCorrelationCode())
                        .build().toUri(), HttpMethod.POST, requestEntity, String.class);
    }

    @Override
    public void deleteAsset(String id) {
        notFoundOrUnauthorizedProtectedDelete(
                resolvePathSegments(API_PATH_SEGMENT, PLUGINS_PATH_SEGMENT, CMS_PATH_SEGMENT,
                        ASSETS_PATH_SEGMENT, id).build().toUri());
    }

    @Override
    public void createFolder(final String folder) {
        this.getRestTemplate().postForEntity(
                resolvePathSegments(API_PATH_SEGMENT, FILE_BROWSER_PATH_SEGMENT, DIRECTORY_PATH_SEGMENT).build()
                        .toUri(),
                new EntandoCoreFolder(folder), Void.class);
    }

    @Override
    public void deleteFolder(final String code) {
        UriComponentsBuilder builder = resolvePathSegments(API_PATH_SEGMENT, FILE_BROWSER_PATH_SEGMENT,
                DIRECTORY_PATH_SEGMENT);
        builder.queryParam("protectedFolder", "false");
        builder.queryParam("currentPath", code);
        notFoundOrUnauthorizedProtectedDelete(builder.build().toUri());
    }

    @Override
    public void createFile(final FileDescriptor descriptor) {
        final String path = Paths.get(descriptor.getFolder(), descriptor.getFilename()).toString();
        final EntandoCoreFile file = new EntandoCoreFile(false, path, descriptor.getFilename(), descriptor.getBase64());
        this.getRestTemplate()
                .postForEntity(
                        resolvePathSegments(API_PATH_SEGMENT, FILE_BROWSER_PATH_SEGMENT, FILE_PATH_SEGMENT).build()
                                .toUri(),
                        file,
                        Void.class);
    }

    @Override
    public void updateFile(FileDescriptor descriptor) {
        final String path = Paths.get(descriptor.getFolder(), descriptor.getFilename()).toString();
        final EntandoCoreFile file = new EntandoCoreFile(false, path, descriptor.getFilename(), descriptor.getBase64());
        this.getRestTemplate()
                .put(resolvePathSegments(API_PATH_SEGMENT, FILE_BROWSER_PATH_SEGMENT, FILE_PATH_SEGMENT).build()
                        .toUri(), file);
    }

    @Override
    public void createCategory(CategoryDescriptor representation) {
        this.getRestTemplate()
                .postForEntity(resolvePathSegments(API_PATH_SEGMENT, CATEGORIES_PATH_SEGMENT).build().toUri(),
                        representation,
                        Void.class);
    }

    @Override
    public void updateCategory(CategoryDescriptor representation) {
        this.getRestTemplate()
                .put(resolvePathSegments(API_PATH_SEGMENT, CATEGORIES_PATH_SEGMENT, representation.getCode()).build()
                                .toUri(),
                        representation);
    }

    @Override
    public void deleteCategory(String code) {
        notFoundOrUnauthorizedProtectedDelete(
                resolvePathSegments(API_PATH_SEGMENT, CATEGORIES_PATH_SEGMENT, code).build().toUri());

    }

    @Override
    public EntandoCoreComponentUsage getCategoryUsage(String code) {
        return this.getComponentUsage(code,
                new String[]{API_PATH_SEGMENT, CATEGORIES_PATH_SEGMENT, code, USAGE_PATH_SEGMENT},
                "category");
    }

    @Override
    public AnalysisReport getEngineAnalysisReport(List<Reportable> reportableList) {

        return this.getAnalysisReport(reportableList, ReportableRemoteHandler.ENTANDO_ENGINE,
                AnalysisReportClientRequestFactory::createEngineAnalysisReportRequest,
                API_PATH_SEGMENT, ANALYSIS_PATH_SEGMENT, COMPONENTS_PATH_SEGMENT, DIFF_PATH_SEGMENT);
    }

    @Override
    public AnalysisReport getCMSAnalysisReport(List<Reportable> reportableList) {

        return this.getAnalysisReport(reportableList, ReportableRemoteHandler.ENTANDO_CMS,
                AnalysisReportClientRequestFactory::createCMSAnalysisReportRequest,
                API_PATH_SEGMENT, ANALYSIS_PATH_SEGMENT, CMS_PATH_SEGMENT, COMPONENTS_PATH_SEGMENT, DIFF_PATH_SEGMENT);
    }

    /**
     * request the AnalysisReport to a particular remote service for a list of Reportable.
     *
     * @param reportableList           the List of Reportable of which get the report
     * @param reportableRemoteHandler  the ReportableRemoteHandler identifying the service to call (useful for error
     *                                 mex)
     * @param factoryRequestCreationFn a function containing the method of the AnalysisReportClientRequestFactory to
     *                                 call to create the request right for the current scope
     * @param pathSegments             the segments composing the path to call
     * @return the received AnalysisReport
     */
    private AnalysisReport getAnalysisReport(List<Reportable> reportableList,
            ReportableRemoteHandler reportableRemoteHandler,
            Function<AnalysisReportClientRequestFactory, AnalysisReportClientRequest> factoryRequestCreationFn,
            String... pathSegments) {

        AnalysisReportClientRequestFactory requestFactory = AnalysisReportClientRequestFactory
                .anAnalysisReportClientRequest().reportableList(reportableList);

        AnalysisReportClientRequest analysisReportClientRequest = factoryRequestCreationFn.apply(requestFactory);

        try {
            ResponseEntity<SimpleRestResponse<AnalysisReport>> reportResponseEntity = this.getRestTemplate()
                    .exchange(resolvePathSegments(pathSegments).build().toUri(),
                            HttpMethod.POST, new HttpEntity<>(analysisReportClientRequest),
                            new ParameterizedTypeReference<>() {
                            });

            if (! reportResponseEntity.getStatusCode().is2xxSuccessful()) {
                throw new ReportAnalysisException(String.format(
                        "An error occurred fetching the %s report analysis", reportableRemoteHandler));
            } else {
                return Optional.ofNullable(reportResponseEntity.getBody())
                        .map(RestResponse::getPayload)
                        .orElseThrow(() -> new ReportAnalysisException(
                                "Empty response received by " + reportableRemoteHandler));
            }
        } catch (Exception e) {
            String message = String.format("An error occurred fetching the %s report analysis",
                    reportableRemoteHandler);
            log.error(message, e);
            throw new ReportAnalysisException(message, e);
        }
    }

    private UriComponentsBuilder resolvePathSegments(String... segments) {
        return UriComponentsBuilder
                .fromUriString(entandoUrl)
                .pathSegment(segments);
    }

    /**
     * asks for component the received component id usage count.
     *
     * @param code             the code of the component of which get the usage count
     * @param endpointUrlParts a String array containing the query segments representing the endpoint url to qurey
     * @return an instance of EntandoCoreComponentUsage containing the number of the entities using the component
     */
    public EntandoCoreComponentUsage getComponentUsage(String code, String[] endpointUrlParts, String componentType) {

        ResponseEntity<SimpleRestResponse<EntandoCoreComponentUsage>> usage = this.getRestTemplate()
                .exchange(resolvePathSegments(endpointUrlParts).build().toUri(), HttpMethod.GET, null,
                        new ParameterizedTypeReference<SimpleRestResponse<EntandoCoreComponentUsage>>() {
                        });

        if (usage.getStatusCode().is2xxSuccessful()) {
            return Optional.ofNullable(usage.getBody())
                    .map(RestResponse::getPayload)
                    .orElseThrow(() -> new ReportAnalysisException(
                            String.format("Empty response received for usage of component type %s with code %s",
                                    componentType, code)));
        } else {
            throw new WebHttpException(usage.getStatusCode(),
                    String.format("Some error occurred while retrieving %s %s usage", componentType, code));
        }
    }

    private void notFoundOrUnauthorizedProtectedDelete(URI url) {
        try {
            this.getRestTemplate().delete(url);
        } catch (RestClientResponseException e) {
            if (isSafeDeleteResponseStatus(e.getRawStatusCode())) {
                return;
            }
            throw e;
        }
    }

    private boolean isSafeDeleteResponseStatus(int status) {
        HttpStatus s = HttpStatus.resolve(status);
        return s != null && (s.is2xxSuccessful()
                || s.equals(HttpStatus.NOT_FOUND)
                || s.equals(HttpStatus.FORBIDDEN));
    }

    private boolean isRetryableResponseStatus(int status) {
        HttpStatus s = HttpStatus.resolve(status);
        return s != null && s.is5xxServerError() && errorsCandidatesToRetry.contains(s);
    }

    private boolean shouldRetry(Object obj, Exception ex, int executionNumber) {
        if (ex instanceof RestClientResponseException) {
            RestClientResponseException e = (RestClientResponseException) ex;
            if (isRetryableResponseStatus(e.getRawStatusCode())) {
                log.info("Error in a REST call to entandoDeApp code:'{}'", e.getRawStatusCode());
                log.debug("Error: ", e);
                return false;
            }
        }
        log.debug("Error in a REST call to entandoDeApp", ex);
        return true;
    }

    private Object genericRestExecutor(Runnable c, int executionNumber) {
        c.run();
        return null;
    }

    private MethodRetryer<Runnable, Object> buildDefaultRetryer() {
        return MethodRetryer.<Runnable, Object>builder()
                .retries(retryNumber)
                .waitFor(backOffPeriod)
                .execMethod(this::genericRestExecutor)
                .checkerMethod(this::shouldRetry)
                .build();
    }

    private static int parseIntOrDefault(String envNameToParse, int defaultValue) {
        String envToParse = Optional.ofNullable(System.getenv(envNameToParse)).orElse("" + defaultValue);
        try {
            return Integer.parseInt(envToParse);
        } catch (NumberFormatException ex) {
            log.error("Error parsing:'{}' from env with name:'{}'", envToParse, envNameToParse, ex);
            return defaultValue;
        }
    }

}
