package org.entando.kubernetes.utils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import org.apache.commons.io.IOUtils;
import org.entando.kubernetes.exception.web.InternalServerException;
import org.springframework.core.io.ClassPathResource;

public class FileUtils {
    public static String readFromFile(String filename) {
        try (InputStream is = new ClassPathResource(filename).getInputStream()){
            return IOUtils.toString(is, Charset.defaultCharset());
        } catch (IOException e) {
            throw new InternalServerException("Error reading file", e);
        }
    }
}
