package org.entando.kubernetes.model.bundle;

import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.entando.kubernetes.exception.digitalexchange.InvalidBundleException;
import org.entando.kubernetes.model.bundle.descriptor.ComponentDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.FileDescriptor;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class BundleReader {

    private final YAMLMapper mapper = new YAMLMapper();
    private final Path bundleBasePath;

    public BundleReader(Path filePath) {
        bundleBasePath = filePath;
    }

    public ComponentDescriptor readBundleDescriptor() throws IOException {
        return readDescriptorFile(BundleProperty.DESCRIPTOR_FILENAME.getValue(), ComponentDescriptor.class);
    }

    public boolean containsResourceFolder() {
        return bundleBasePath.resolve(BundleProperty.RESOURCES_FOLDER_PATH.getValue()).toFile().isDirectory();
    }

    public String getBundleCode() throws IOException {
        return readBundleDescriptor().getCode();
    }

    public List<String> getResourceFolders() {
        return getResourceOfType(Files::isDirectory);
    }


    public List<String> getResourceFiles() {
        return getResourceOfType(Files::isRegularFile);
    }

    private List<String> getResourceOfType(Predicate<Path> checkFunction) {
        List<Path> resources;
        Path resourcePath = bundleBasePath.resolve("resources/");
        try (Stream<Path> paths = Files.walk(resourcePath)) {
            resources = paths
                    .filter(checkFunction)
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
        log.debug("Reading file {}", fileName);
        if (!bundleBasePath.resolve(fileName).toFile().exists()) {
            throw new InvalidBundleException(String.format("File with name %s not found in the bundle", fileName));
        }
    }

}
