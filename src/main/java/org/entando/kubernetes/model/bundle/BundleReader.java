package org.entando.kubernetes.model.bundle;

import static java.nio.file.Files.newInputStream;
import static java.nio.file.StandardOpenOption.READ;

import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.entando.kubernetes.exception.digitalexchange.InvalidBundleException;
import org.entando.kubernetes.model.bundle.descriptor.ComponentDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.FileDescriptor;

@Slf4j
public class BundleReader {

    private final YAMLMapper mapper = new YAMLMapper();
    private final Path bundleBasePath;

    public BundleReader(Path filePath) throws IOException {
        bundleBasePath = filePath;
    }

    public String getBundleId() throws IOException {
        ComponentDescriptor bundleDescriptor = readBundleDescriptor();
        return bundleDescriptor.getCode();
    }

    public ComponentDescriptor readBundleDescriptor() throws IOException {
        return readDescriptorFile(BundleProperty.DESCRIPTOR_FILENAME.getValue(), ComponentDescriptor.class);
    }


    private Map<String, File> rebaseEntriesNamesOnComponentDescriptorFile(Map<String, File> tarEntries) {
        Optional<String> descriptorPath = tarEntries.keySet().stream()
                .filter(s -> s.endsWith(BundleProperty.DESCRIPTOR_FILENAME.getValue()))
                .min(Comparator.comparing(String::length));
        String descriptorFolder = FilenameUtils.getPath(
                descriptorPath.orElseThrow(() -> new InvalidBundleException("No descriptor file found in the")));

        Map<String, File> rebasedEntries = new HashMap<>();
        Iterator<Entry<String, File>> iterator = tarEntries.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, File> entry = iterator.next();
            rebasedEntries.put(entry.getKey().replace(descriptorFolder, ""), entry.getValue());
        }
        return rebasedEntries;
    }

    public void destroy() {
        this.bundleBasePath.toFile().delete();
    }

    public boolean containsResourceFolder() {
        return bundleBasePath.resolve(BundleProperty.RESOURCES_FOLDER_PATH.getValue()).toFile().isDirectory();
    }

    public List<String> getResourceFolders() {
        return getResourceOfType(Files::isDirectory);
    }


    public List<String> getResourceFiles() {
        return getResourceOfType(Files::isRegularFile);
    }

    private List<String> getResourceOfType(Function<Path, Boolean> checkFunction) {
        List<Path> resources;
        Path resourcePath = bundleBasePath.resolve("resources/");
        try (Stream<Path> paths = Files.walk(resourcePath)) {
            resources = paths
                    .filter(checkFunction::apply)
                    .filter(p -> p != resourcePath)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            resources = Collections.emptyList();
        }
        return resources.stream().map(bundleBasePath::relativize).map(Path::toString).collect(Collectors.toList());

    }

    public <T> T readDescriptorFile(final String fileName, final Class<T> clazz) throws IOException {
        try (InputStream fis = new FileInputStream(bundleBasePath.resolve(fileName).toFile())) {
            return readDescriptorFile(fis, clazz);
        }
    }

    public String readFileAsString(String fileName) throws IOException {
        verifyFileExistance(fileName);
        try (InputStream fis = new FileInputStream(bundleBasePath.resolve(fileName).toFile());
             StringWriter writer = new StringWriter()) {
            IOUtils.copy(fis, writer, StandardCharsets.UTF_8);
            return writer.toString();
        }
    }

    public FileDescriptor getResourceFileAsDescriptor(final String fileName) throws IOException {
        verifyFileExistance(fileName);
        File f = bundleBasePath.resolve(fileName).toFile();
        try (final ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            IOUtils.copy(new FileInputStream(f), outputStream);
            final String base64 = Base64.encodeBase64String(outputStream.toByteArray());
            final String filename = FilenameUtils.getName(fileName);
            final String folder = FilenameUtils.getPath(fileName);
            return new FileDescriptor(folder, filename, base64);
        }
    }

    private <T> T readDescriptorFile(final InputStream file, Class<T> clazz) throws IOException {
        return mapper.readValue(file, clazz);
    }

    private void verifyFileExistance(String fileName) {
        log.info("Reading file {}", fileName);
        if (!bundleBasePath.resolve(fileName).toFile().exists()) {
            throw new InvalidBundleException(String.format("File with name %s not found in the bundle", fileName));
        }
//        if (!tarEntries.containsKey(fileName)) {
//            throw new InvalidBundleException(String.format("File with name %s not found in the bundle", fileName));
//        }
    }

}
