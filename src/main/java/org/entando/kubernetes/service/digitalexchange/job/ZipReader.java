package org.entando.kubernetes.service.digitalexchange.job;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.apache.commons.io.IOUtils;
import org.entando.kubernetes.service.digitalexchange.job.model.Descriptor;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ZipReader {

    private final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
    private final Map<String, ZipEntry> zipEntries;
    private final ZipFile zipFile;

    public ZipReader(final ZipFile zipFile) {
        this.zipFile = zipFile;
        this.zipEntries = Collections.list(zipFile.entries()).stream()
                .collect(Collectors.toMap(ZipEntry::getName, self -> self));
    }

    public <T extends Descriptor> Optional<T> readDescriptorFile(final String fileName, final Class<T> clazz) throws IOException {
        final ZipEntry zipEntry = zipEntries.get(fileName);
        return zipEntry != null ? Optional.of(readDescriptorFile(zipFile.getInputStream(zipEntry), clazz)) : Optional.empty();
    }

    public Optional<String> readFileAsString(final String fileName) throws IOException {
        final ZipEntry zipEntry = zipEntries.get(fileName);
        if (zipEntry == null) return Optional.empty();

        try (final StringWriter writer = new StringWriter()) {
            IOUtils.copy(zipFile.getInputStream(zipEntry), writer);
            return Optional.of(writer.toString());
        }
    }

    private <T extends Descriptor> T readDescriptorFile(final InputStream file, Class<T> clazz) throws IOException {
        return mapper.readValue(file, clazz);
    }

}
