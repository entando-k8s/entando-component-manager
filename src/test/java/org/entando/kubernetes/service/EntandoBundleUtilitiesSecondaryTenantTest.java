package org.entando.kubernetes.service;

import static org.entando.kubernetes.stubhelper.PluginStubHelper.SECONDARY_TENANT_CODE;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import io.fabric8.kubernetes.api.model.EnvVarSource;
import io.fabric8.kubernetes.api.model.EnvVarSourceBuilder;
import java.util.Collections;
import java.util.List;
import org.entando.kubernetes.exception.EntandoValidationException;
import org.entando.kubernetes.model.bundle.descriptor.plugin.EnvironmentVariable;
import org.entando.kubernetes.service.digitalexchange.BundleUtilities;
import org.entando.kubernetes.stubhelper.PluginStubHelper;
import org.entando.kubernetes.utils.TenantSecondaryContextJunitExt;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@Tag("unit")
@ExtendWith(TenantSecondaryContextJunitExt.class)
class EntandoBundleUtilitiesSecondaryTenantTest {

    @Test
    void secretOwnedByPrimaryResultsInExceptionBeingThrownIfUsedInOtherTenant() {
        final String ENVIRONMENT_VARIABLE = "ENVIRONMENT_VARIABLE";

        EnvVarSource envVarSource = new EnvVarSourceBuilder()
                .withNewSecretKeyRef("key", "pn-hasldk12-8dsjahj2-mypluginname", null)
                .build();
        EnvVar customEnvvar = new EnvVarBuilder()
                .withName(ENVIRONMENT_VARIABLE)
                .withValueFrom(envVarSource)
                .build();

        final List<EnvironmentVariable> environmentVariableList = PluginStubHelper.stubEnvironmentVariables();
        final List<EnvVar> singletonList
                = Collections.singletonList(customEnvvar);
        EntandoValidationException exception = assertThrows(EntandoValidationException.class,
                () -> BundleUtilities.assemblePluginEnvVars(environmentVariableList, singletonList));
        assertTrue(exception.getMessage().contains("Cannot reference a primary secret on the non-primary tenant"));
        assertTrue(exception.getMessage().contains(SECONDARY_TENANT_CODE));
        assertTrue(exception.getMessage().contains(ENVIRONMENT_VARIABLE));
    }

}
