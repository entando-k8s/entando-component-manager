package org.entando.kubernetes.model.bundle;

import static org.apache.commons.lang3.StringUtils.isEmpty;

import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.entando.kubernetes.model.bundle.descriptor.FileDescriptor;

public class ZipReader {

    private final YAMLMapper mapper = new YAMLMapper();
    private final Map<String, ZipEntry> zipEntries;
    private final ZipFile zipFile;
    private static final String RESOURCES_FOLDER_NAME = "resources";
    private static final String RESOURCES_FOLDER_PATH = "resources/";

    public ZipReader(final ZipFile zipFile) {
        this.zipFile = zipFile;
        this.zipEntries = zipFile.stream()
                .collect(Collectors.toMap(ZipEntry::getName, self -> self));
    }

    public boolean containsResourceFolder() {
        return zipEntries.keySet().stream().anyMatch(n -> n.startsWith(RESOURCES_FOLDER_PATH));
    }

    public List<String> getResourceFolders() {
        return zipEntries.keySet().stream().filter(path -> path.startsWith(RESOURCES_FOLDER_NAME))
                .map(FilenameUtils::getFullPath) // Not always directory entry is available as a single path in the zip
                .distinct()
                .filter(path -> !path.equals(RESOURCES_FOLDER_NAME) && !path.equals(RESOURCES_FOLDER_PATH))
                .map(path -> path.substring(RESOURCES_FOLDER_PATH.length(), path.length() - 1))
                .flatMap(this::getIntermediateFolders)
                .distinct()
                .sorted(Comparator.comparing(String::length))
                .collect(Collectors.toList());
    }

    /**
     * Given a directory, returns a stream of all the intermediate directories e.g. /static/img/svg/full-size -> {static, static/img,
     * static/img/svg, static/img/svg/full-size}
     *
     * @param path The path to use to extract intermediate directories
     * @return Stream of String
     */
    private Stream<String> getIntermediateFolders(String path) {
        List<Path> newPaths = new ArrayList<>();
        Paths.get(path).iterator().forEachRemaining(newPaths::add);
        String[] tmpPaths = new String[newPaths.size()];
        tmpPaths[0] = newPaths.get(0).toString();
        for (int i = 1; i < newPaths.size(); i++) {
            tmpPaths[i] = Paths.get(tmpPaths[i - 1], newPaths.get(i).toString()).toString();
        }
        return Stream.of(tmpPaths);
    }

    public List<String> getResourceFiles() {
        return zipEntries.keySet().stream().filter(path -> path.startsWith(RESOURCES_FOLDER_NAME))
                .filter(path -> !zipEntries.get(path).isDirectory())
                .collect(Collectors.toList());
    }

    public <T> T readDescriptorFile(final String fileName, final Class<T> clazz) throws IOException {
        final ZipEntry zipEntry = getFile(fileName);
        return readDescriptorFile(zipFile.getInputStream(zipEntry), clazz);
    }

    private <T> T readDescriptorFile(final InputStream file, Class<T> clazz) throws IOException {
        return mapper.readValue(file, clazz);
    }

    public String readFileAsString(final String folder, final String fileName) throws IOException {
        final ZipEntry zipEntry = getFile(isEmpty(folder) ? fileName : folder + "/" + fileName);

        try (final StringWriter writer = new StringWriter()) {
            IOUtils.copy(zipFile.getInputStream(zipEntry), writer);
            return writer.toString();
        }
    }

    public FileDescriptor readFileAsDescriptor(final String fileName) throws IOException {
        final ZipEntry zipEntry = getFile(fileName);

        try (final ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            IOUtils.copy(zipFile.getInputStream(zipEntry), outputStream);
            final String base64 = Base64.encodeBase64String(outputStream.toByteArray());
            final String filename = fileName.substring(fileName.lastIndexOf('/') + 1);
            final String folder = fileName.lastIndexOf('/') >= RESOURCES_FOLDER_PATH.length()
                    ? fileName.substring("resources/".length(), fileName.lastIndexOf('/'))
                    : "";
            return new FileDescriptor(folder, filename, base64);
        }
    }

    private ZipEntry getFile(final String fileName) throws FileNotFoundException {
        final ZipEntry zipEntry = zipEntries.get(fileName);
        if (zipEntry == null) {
            throw new FileNotFoundException("File " + fileName + " not found");
        }
        return zipEntry;
    }

}
