package org.entando.kubernetes.client.model;

import org.entando.kubernetes.model.digitalexchange.ImageConverter;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
public class ImageConverterTest {


    @Test
    public void shouldConvertBackAndForthCorrectly() {
        ImageConverter converter = new ImageConverter();
        String imageToConvert = readResourceAsString("/images/base64TestImage");

        String result = converter.convertToEntityAttribute(converter.convertToDatabaseColumn(imageToConvert));

        assertThat(result).isEqualTo(imageToConvert);
    }

    public String readResourceAsString(String resourcePath) {

        try {
            Path rp = Paths.get(this.getClass().getResource(resourcePath).toURI());
            return new String(Files.readAllBytes(rp));
        } catch (IOException | URISyntaxException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
}
