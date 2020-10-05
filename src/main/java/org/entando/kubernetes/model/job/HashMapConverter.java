package org.entando.kubernetes.model.job;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Map;
import javax.persistence.AttributeConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HashMapConverter implements AttributeConverter<Map<String, String>, String> {

    private static Logger log = LoggerFactory.getLogger(HashMapConverter.class);

    private ObjectMapper objectMapper;

    @Override
    public String convertToDatabaseColumn(Map<String, String> bundleMetadata) {

        String customerInfoJson = null;
        if (bundleMetadata != null) {
            try {
                customerInfoJson = objectMapper.writeValueAsString(bundleMetadata);
            } catch (final JsonProcessingException e) {
                log.error("JSON writing error", e);
            }
        }

        return customerInfoJson;
    }

    @Override
    public Map<String, String> convertToEntityAttribute(String bundleMetadataJson) {

        Map<String, String> customerInfo = null;
        if (bundleMetadataJson != null) {
            try {
                customerInfo = objectMapper.readValue(bundleMetadataJson, Map.class);
            } catch (final IOException e) {
                log.error("JSON reading error", e);
            }
        }
        return customerInfo;
    }

}


