package org.entando.kubernetes.service.digitalexchange.job;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.entando.kubernetes.service.digitalexchange.job.model.Descriptor;
import org.entando.kubernetes.service.digitalexchange.job.model.FileDescriptor;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static org.apache.commons.lang3.StringUtils.isEmpty;

public class ZipReader {

    private final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
    private final Map<String, ZipEntry> zipEntries;
    private final ZipFile zipFile;

    public ZipReader(final ZipFile zipFile) {
        this.zipFile = zipFile;
        this.zipEntries = Collections.list(zipFile.entries()).stream()
                .collect(Collectors.toMap(ZipEntry::getName, self -> self));
    }

    public List<String> getResourceFolders() {
        return zipEntries.keySet().stream().filter(path -> path.startsWith("resources"))
                .filter(path -> zipEntries.get(path).isDirectory())
                .filter(path -> !path.equals("resources") && !path.equals("resources/"))
                .map(path -> path.substring("resources/".length(), path.length() - 1))
                .sorted(Comparator.comparing(String::length))
                .collect(Collectors.toList());
    }

    public List<String> getResourceFiles() {
        return zipEntries.keySet().stream().filter(path -> path.startsWith("resources"))
                .filter(path -> !zipEntries.get(path).isDirectory())
                .collect(Collectors.toList());
    }

    public <T extends Descriptor> Optional<T> readDescriptorFile(final String fileName, final Class<T> clazz) throws IOException {
        final ZipEntry zipEntry = zipEntries.get(fileName);
        return zipEntry != null ? Optional.of(readDescriptorFile(zipFile.getInputStream(zipEntry), clazz)) : Optional.empty();
    }

    public Optional<String> readFileAsString(final String folder, final String fileName) throws IOException {
        final ZipEntry zipEntry = zipEntries.get(isEmpty(folder) ? fileName : folder + "/" + fileName);
        if (zipEntry == null) return Optional.empty();

        try (final StringWriter writer = new StringWriter()) {
            IOUtils.copy(zipFile.getInputStream(zipEntry), writer);
            return Optional.of(writer.toString());
        }
    }

    public Optional<FileDescriptor> readFileAsDescriptor(final String fileName) throws IOException {
        final ZipEntry zipEntry = zipEntries.get(fileName);
        if (zipEntry == null) return Optional.empty();

        try (final ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            IOUtils.copy(zipFile.getInputStream(zipEntry), outputStream);
            final String base64 = Base64.encodeBase64String(outputStream.toByteArray());
            final String filename = fileName.substring(fileName.lastIndexOf('/') + 1);
            final String folder = fileName.substring("resources/".length(), fileName.lastIndexOf('/'));
            return Optional.of(new FileDescriptor(folder, filename, base64));
        }
    }

    private <T extends Descriptor> T readDescriptorFile(final InputStream file, Class<T> clazz) throws IOException {
        return mapper.readValue(file, clazz);
    }

}
