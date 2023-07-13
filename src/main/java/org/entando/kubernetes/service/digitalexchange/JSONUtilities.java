package org.entando.kubernetes.service.digitalexchange;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.entando.kubernetes.model.bundle.descriptor.Descriptor;
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

    public static <T extends Descriptor> Descriptor deserializeDescriptor(String value, Class<T> clazz) {

        try {
            return deserializeDescriptorOrThrow(value, clazz);
        } catch (JsonProcessingException ex) {
            String err = String.format(
                    "error deserializeDescriptor with Class %s from value:'%s' error:'%s'",
                    clazz,
                    value, ex.getMessage());

            log.error(err);
            throw Problem.valueOf(Status.INTERNAL_SERVER_ERROR, err);
        }
    }

    public static <T extends Descriptor> Descriptor deserializeDescriptorOrThrow(String value, Class<T> clazz)
            throws JsonProcessingException {

        ObjectMapper jsonMapper = new ObjectMapper();

        return jsonMapper.readValue(value, clazz);
    }

}
