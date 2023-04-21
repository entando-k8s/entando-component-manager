package org.entando.kubernetes.config;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.extensions.IngressSpec;
import java.util.List;
import oracle.jdbc.logging.annotations.Feature;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.startup.Tomcat;
import org.entando.kubernetes.client.k8ssvc.K8SServiceClient.ApplicationStatus;
import org.entando.kubernetes.client.model.EntandoAppPluginLinkJavaNative;
import org.entando.kubernetes.client.model.EntandoDeBundleJavaNative;
import org.entando.kubernetes.client.model.EntandoPluginJavaNative;
import org.entando.kubernetes.client.model.IngressJavaNative;
import org.entando.kubernetes.config.JavaNativeHintConfiguration.LiquibaseRuntimeHints;
import org.entando.kubernetes.liquibase.GenerateUUIDForNullColumn;
import org.entando.kubernetes.model.bundle.descriptor.widget.KeepAsJsonDeserializer;
import org.entando.kubernetes.model.bundle.descriptor.widget.WidgetDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.widget.WidgetDescriptor.ApiClaim;
import org.entando.kubernetes.model.bundle.descriptor.widget.WidgetDescriptor.ConfigUi;
import org.entando.kubernetes.model.bundle.descriptor.widget.WidgetDescriptor.DescriptorMetadata;
import org.entando.kubernetes.model.bundle.descriptor.widget.WidgetDescriptor.Param;
import org.entando.kubernetes.model.bundle.descriptor.widget.WidgetDescriptor.WidgetExt;
import org.entando.kubernetes.model.common.EntandoCustomResourceStatus;
import org.entando.kubernetes.model.debundle.EntandoDeBundle;
import org.entando.kubernetes.model.debundle.EntandoDeBundleDetails;
import org.entando.kubernetes.model.debundle.EntandoDeBundleSpec;
import org.entando.kubernetes.model.debundle.EntandoDeBundleTag;
import org.entando.kubernetes.model.link.EntandoAppPluginLinkSpec;
import org.entando.kubernetes.model.plugin.EntandoPluginSpec;
import org.entando.kubernetes.model.web.request.Filter;
import org.entando.kubernetes.model.web.request.FilterOperator;
import org.entando.kubernetes.model.web.request.FilterType;
import org.entando.kubernetes.model.web.request.PagedListRequest;
import org.entando.kubernetes.model.web.response.PagedMetadata;
import org.entando.kubernetes.service.digitalexchange.templating.WidgetTemplateGeneratorService;
import org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy;
import org.hibernate.engine.transaction.jta.platform.internal.NoJtaPlatform;
import org.postgresql.util.PGInterval;
import org.postgresql.util.PGobject;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.orm.jpa.hibernate.SpringImplicitNamingStrategy;
import org.springframework.boot.web.embedded.tomcat.ConfigurableTomcatWebServerFactory;
import org.springframework.boot.web.embedded.tomcat.TomcatConnectorCustomizer;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.servlet.server.ConfigurableServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportRuntimeHints;
import org.springframework.core.env.Environment;

@Configuration
@ImportRuntimeHints(LiquibaseRuntimeHints.class)
public class JavaNativeHintConfiguration {

    @Value("${server.tomcat.relaxed-query-chars}")
    private List<Character> chars;

    public static class LiquibaseRuntimeHints implements RuntimeHintsRegistrar {

        @Override
        public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
            hints.resources().registerPattern("application.properties");

            hints.resources().registerPattern("db/changelog/db.changelog-master.yaml");
            hints.resources().registerPattern("db/changelog/changes/*");

            hints.reflection().registerType(GenerateUUIDForNullColumn.class, MemberCategory.values());

            //fix t orm problems
            hints.reflection().registerType(NoJtaPlatform.class, MemberCategory.values());
            hints.reflection().registerType(SpringImplicitNamingStrategy.class, MemberCategory.values());
            hints.reflection().registerType(CamelCaseToUnderscoresNamingStrategy.class, MemberCategory.values());

            // internal class how to add ?
            //hints.reflection().registerType(BeanValidationIntegrator.class, MemberCategory.values());
            hints.reflection().registerTypeIfPresent(this.getClass().getClassLoader(),
                    "org.hibernate.cfg.beanvalidation.TypeSafeActivator", MemberCategory.values());

            hints.reflection().registerType(PGobject.class, MemberCategory.values());
            hints.reflection().registerType(PGInterval.class, MemberCategory.values());
            hints.reflection().registerType(Feature.class, MemberCategory.values());

            hints.serialization().registerType(ApplicationStatus.class);

            // getting work /all/widgets?filter
            hints.reflection().registerType(PagedListRequest.class, MemberCategory.values());
            hints.reflection().registerType(Filter.class, MemberCategory.values());
            hints.reflection().registerType(FilterOperator.class, MemberCategory.values());
            hints.reflection().registerType(FilterType.class, MemberCategory.values());
            hints.reflection().registerType(WidgetDescriptor.class, MemberCategory.values());
            hints.reflection().registerType(KeepAsJsonDeserializer.class, MemberCategory.values());
            hints.reflection().registerType(ConfigUi.class, MemberCategory.values());
            hints.reflection().registerType(ApiClaim.class, MemberCategory.values());
            hints.reflection().registerType(Param.class, MemberCategory.values());
            hints.reflection().registerType(DescriptorMetadata.class, MemberCategory.values());
            hints.reflection()
                    .registerType(DescriptorMetadata.DescriptorMetadataBuilder.class, MemberCategory.values());
            hints.reflection().registerType(WidgetExt.class, MemberCategory.values());
            hints.reflection().registerType(WidgetTemplateGeneratorService.SystemParams.SystemParamsBuilder.class,
                    MemberCategory.values());
            hints.reflection().registerType(WidgetTemplateGeneratorService.SystemParams.class,
                    MemberCategory.values());
            hints.reflection()
                    .registerType(WidgetTemplateGeneratorService.ApiUrl.ApiUrlBuilder.class, MemberCategory.values());
            hints.reflection()
                    .registerType(WidgetTemplateGeneratorService.ApiUrl.class, MemberCategory.values());
            hints.reflection().registerType(PagedMetadata.class, MemberCategory.values());

            // getting work /k8s/bundles
            hints.reflection().registerType(EntandoDeBundleJavaNative.class, MemberCategory.values());
            hints.reflection().registerType(EntandoDeBundle.class, MemberCategory.values());
            hints.reflection().registerType(EntandoDeBundleSpec.class, MemberCategory.values());
            hints.reflection().registerType(EntandoCustomResourceStatus.class, MemberCategory.values());
            hints.reflection().registerType(ObjectMeta.class, MemberCategory.values());
            hints.reflection().registerType(EntandoDeBundleDetails.class, MemberCategory.values());
            hints.reflection().registerType(EntandoDeBundleTag.class, MemberCategory.values());

            hints.reflection().registerType(EntandoAppPluginLinkJavaNative.class, MemberCategory.values());
            hints.reflection().registerType(EntandoAppPluginLinkSpec.class, MemberCategory.values());

            hints.reflection().registerType(EntandoPluginJavaNative.class, MemberCategory.values());
            hints.reflection().registerType(EntandoPluginSpec.class, MemberCategory.values());

            hints.reflection().registerType(IngressJavaNative.class, MemberCategory.values());
            hints.reflection().registerType(IngressSpec.class, MemberCategory.values());

            // not working char relax []
            hints.reflection()
                    .registerType(CustomWebServerAllowingSquareBracketsInParameters.class, MemberCategory.values());
            hints.reflection().registerType(Tomcat.class, MemberCategory.values());
        }

    }


    public static class CustomWebServerAllowingSquareBracketsInParameters implements
            WebServerFactoryCustomizer<ConfigurableTomcatWebServerFactory> {

        @Override
        public void customize(ConfigurableTomcatWebServerFactory factory) {
            String relaxedQueryChars = "[]";
            System.out.println("Tomcat customize for chars:'" + relaxedQueryChars + "'");

            factory.addConnectorCustomizers(new TomcatConnectorCustomizer[]{
                    (connector) -> {
                        System.out.println("Tomcat execute customizzer relaxedQueryChars:'" + relaxedQueryChars + "'");
                        connector.setProperty("relaxedQueryChars", relaxedQueryChars);
                    }
            });
            factory.addConnectorCustomizers(new TomcatConnectorCustomizer[]{
                    (connector) -> {
                        System.out.println("Tomcat execute customizzer relaxedPathChars:'" + relaxedQueryChars + "'");
                        connector.setProperty("relaxedPathChars", relaxedQueryChars);
                    }
            });

        }
    }

    @Bean
    public WebServerFactoryCustomizer customizerAllowSquareBracketsI(Environment environment,
            ServerProperties serverProperties) {
        return new CustomWebServerAllowingSquareBracketsInParameters();
    }

    @Bean
    public ConfigurableServletWebServerFactory webServerFactory() {
        System.setProperty("tomcat.util.http.parser.HttpParser.requestTargetAllow", "{}");

        TomcatServletWebServerFactory factory = new TomcatServletWebServerFactory();
        factory.addConnectorCustomizers(new TomcatConnectorCustomizer() {
            @Override
            public void customize(Connector connector) {
                connector.setProperty("relaxedQueryChars", "|{}[]");
            }
        });
        return factory;
    }

}
