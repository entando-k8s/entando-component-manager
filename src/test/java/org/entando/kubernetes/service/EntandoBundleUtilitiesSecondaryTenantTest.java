package org.entando.kubernetes.service;

import static org.entando.kubernetes.stubhelper.PluginStubHelper.SECONDARY_TENANT_CODE;
import static org.entando.kubernetes.stubhelper.PluginStubHelper.TEST_ENV_VAR_2_NAME;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.fabric8.kubernetes.api.model.EnvVar;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import org.entando.kubernetes.exception.EntandoValidationException;
import org.entando.kubernetes.model.bundle.descriptor.plugin.EnvironmentVariable;
import org.entando.kubernetes.model.bundle.descriptor.plugin.PluginDescriptor;
import org.entando.kubernetes.model.bundle.downloader.DownloadedBundle;
import org.entando.kubernetes.model.bundle.reader.BundleReader;
import org.entando.kubernetes.service.digitalexchange.BundleUtilities;
import org.entando.kubernetes.stubhelper.BundleStubHelper;
import org.entando.kubernetes.stubhelper.PluginStubHelper;
import org.entando.kubernetes.utils.TenantSecondaryContextJunitExt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.core.io.ClassPathResource;

@Tag("unit")
@ExtendWith(TenantSecondaryContextJunitExt.class)
class EntandoBundleUtilitiesSecondaryTenantTest {

    private BundleReader bundleReader;
    private DownloadedBundle downloadedBundle;
    private Path bundleFolder;

    public static final String ENVIRONMENT_VARIABLE = "ENVIRONMENT_VARIABLE";

    @BeforeEach
    public void readNpmPackage() throws IOException {
        bundleFolder = new ClassPathResource("bundle").getFile().toPath();
        downloadedBundle = new DownloadedBundle(bundleFolder, BundleStubHelper.BUNDLE_DIGEST);
        bundleReader = new BundleReader(downloadedBundle, BundleStubHelper.stubEntandoDeBundle());
    }

    @Test
    void secretOwnedByPrimaryResultsInExceptionBeingThrownIfUsedInOtherTenant() throws IOException {
        final EnvVar customEnvvar = PluginStubHelper.stubEnvironmentVariableWithSecret(ENVIRONMENT_VARIABLE,
                "pn-a1b2c3d4-c0f69e19-mypluginname");
        final List<EnvironmentVariable> environmentVariableList = PluginStubHelper.stubEnvironmentVariables();
        final List<EnvVar> singletonList
                = Collections.singletonList(customEnvvar);
        final PluginDescriptor descriptor = PluginStubHelper.stubPluginDescriptorV5();
        descriptor.setEnvironmentVariables(environmentVariableList);

        EntandoValidationException exception = assertThrows(EntandoValidationException.class,
                () -> BundleUtilities.assemblePluginEnvVars(descriptor, singletonList));
        assertTrue(exception.getMessage().contains("One or more malformed secrets were detected on tenant"));
        assertTrue(exception.getMessage().contains(SECONDARY_TENANT_CODE));
        assertTrue(exception.getMessage().contains(ENVIRONMENT_VARIABLE));
    }

    @Test
    void secretOwnedBySecondaryTenantsDoesNotThrowExceptionIfNotMalformed() throws IOException {
        final EnvVar customEnvvar = PluginStubHelper.stubEnvironmentVariableWithSecret(ENVIRONMENT_VARIABLE,
                "pn-a1b2c3d4-c0f69e19-24f085aa-mypluginn_ame");
        final List<EnvironmentVariable> environmentVariableList = PluginStubHelper.stubEnvironmentVariables(
                TEST_ENV_VAR_2_NAME, "pn-a1b2c3d4-c0f69e19-24f085aa-another_one");
        final List<EnvVar> singletonList
                = Collections.singletonList(customEnvvar);
        final PluginDescriptor descriptor = PluginStubHelper.stubPluginDescriptorV5();
        descriptor.setEnvironmentVariables(environmentVariableList);

        final List<EnvVar> envVars = assertDoesNotThrow(() -> BundleUtilities.assemblePluginEnvVars(descriptor, singletonList));
        assertNotNull(envVars);
        assertFalse(envVars.isEmpty());
        assertEquals(4, envVars.size());
    }

}
