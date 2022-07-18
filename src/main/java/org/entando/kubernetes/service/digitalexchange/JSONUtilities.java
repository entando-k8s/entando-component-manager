package org.entando.kubernetes.service.digitalexchange;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import io.fabric8.zjsonpatch.internal.guava.Strings;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.entando.kubernetes.exception.EntandoComponentManagerException;
import org.entando.kubernetes.exception.EntandoValidationException;
import org.entando.kubernetes.model.bundle.BundleProperty;
import org.entando.kubernetes.model.bundle.BundleType;
import org.entando.kubernetes.model.bundle.EntandoBundleVersion;
import org.entando.kubernetes.model.bundle.descriptor.Descriptor;
import org.entando.kubernetes.model.bundle.descriptor.DescriptorVersion;
import org.entando.kubernetes.model.bundle.descriptor.DockerImage;
import org.entando.kubernetes.model.bundle.descriptor.VersionedDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.plugin.EnvironmentVariable;
import org.entando.kubernetes.model.bundle.descriptor.plugin.PluginDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.plugin.PluginDescriptorV1Role;
import org.entando.kubernetes.model.bundle.reader.BundleReader;
import org.entando.kubernetes.model.common.DbmsVendor;
import org.entando.kubernetes.model.common.ExpectedRole;
import org.entando.kubernetes.model.common.Permission;
import org.entando.kubernetes.model.debundle.EntandoDeBundle;
import org.entando.kubernetes.model.debundle.EntandoDeBundleDetails;
import org.entando.kubernetes.model.plugin.EntandoPlugin;
import org.entando.kubernetes.model.plugin.EntandoPluginBuilder;
import org.entando.kubernetes.model.plugin.PluginSecurityLevel;
import org.entando.kubernetes.validator.ImageValidator;
import org.entando.kubernetes.validator.ValidationFunctions;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.zalando.problem.Problem;
import org.zalando.problem.Status;

@UtilityClass
@Slf4j
public class JSONUtilities {

    public static String serializeDescriptor(Descriptor descriptor) {

        ObjectMapper jsonMapper = new ObjectMapper();

        try {
            return jsonMapper.writeValueAsString(descriptor);
        } catch (JsonProcessingException ex) {
            String err = String.format(
                    "error unmarshalling %s from object with code:'%s' error:'%s'",
                    descriptor.getClass().getSimpleName(),
                    descriptor.getComponentKey().getKey(), ex.getMessage());

            log.error(err);
            throw Problem.valueOf(Status.INTERNAL_SERVER_ERROR, err);
        }
    }
}
