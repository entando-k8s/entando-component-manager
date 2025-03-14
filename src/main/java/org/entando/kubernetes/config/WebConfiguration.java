package org.entando.kubernetes.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.format.support.FormattingConversionService;
import org.springframework.lang.NonNull;
import org.springframework.web.accept.ContentNegotiationManager;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.servlet.resource.ResourceUrlProvider;

@Configuration
@Profile("!test")
public class WebConfiguration extends WebMvcConfigurationSupport {
    @Override
    public RequestMappingHandlerMapping requestMappingHandlerMapping(
            @NonNull ContentNegotiationManager contentNegotiationManager,
            @NonNull FormattingConversionService conversionService,
            @NonNull ResourceUrlProvider resourceUrlProvider
    ) {
        RequestMappingHandlerMapping handlerMapping = super.requestMappingHandlerMapping(contentNegotiationManager, conversionService, resourceUrlProvider);
        handlerMapping.setAlwaysUseFullPath(false);
        return handlerMapping;
    }
}