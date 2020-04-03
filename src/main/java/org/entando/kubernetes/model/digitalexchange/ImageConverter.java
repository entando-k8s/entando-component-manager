package org.entando.kubernetes.model.digitalexchange;

import java.util.Base64;
import javax.persistence.AttributeConverter;

public class ImageConverter implements AttributeConverter<String, byte[]> {

    @Override
    public byte[] convertToDatabaseColumn(String s) {
        return s != null ? Base64.getMimeDecoder().decode(s) : null;
    }

    @Override
    public String convertToEntityAttribute(byte[] bytes) {
        return bytes != null ? Base64.getMimeEncoder().encodeToString(bytes) : "";
    }
}
