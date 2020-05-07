package org.entando.kubernetes.model.digitalexchange;

import javax.persistence.AttributeConverter;

public class ImageConverter implements AttributeConverter<String, byte[]> {

    @Override
    public byte[] convertToDatabaseColumn(String s) {
        return s != null ? s.getBytes() : null;
    }

    @Override
    public String convertToEntityAttribute(byte[] bytes) {
        return bytes != null ? new String(bytes) : "";
    }
}
