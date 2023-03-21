package org.entando.kubernetes.model.bundle.reader;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.entando.kubernetes.exception.EntandoComponentManagerException;
import org.entando.kubernetes.exception.digitalexchange.InvalidBundleException;
import org.entando.kubernetes.model.bundle.BundleProperty;
import org.entando.kubernetes.model.bundle.descriptor.BundleDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.DescriptorVersion;
import org.entando.kubernetes.model.bundle.descriptor.FileDescriptor;
import org.entando.kubernetes.model.bundle.downloader.DownloadedBundle;
import org.entando.kubernetes.model.debundle.EntandoDeBundle;
import org.entando.kubernetes.service.digitalexchange.BundleUtilities;
import org.entando.kubernetes.validator.descriptor.BundleDescriptorValidator;

@Slf4j
public class BundleReader {

    private final YAMLMapper mapper = new YAMLMapper();
    private final Path bundleBasePath;
    private final String bundleDigest;
    private EntandoDeBundle entandoDeBundle;
    private BundleDescriptor bundleDescriptor;
    private String bundleUrl;
    private String bundleName;

    public BundleReader(Path filePath) {
        this.bundleBasePath = filePath;
        this.entandoDeBundle = null;
        this.bundleDigest = "";
    }

    public BundleReader(Path filePath, String bundleUrl) {
        this.bundleBasePath = filePath;
        this.bundleUrl = bundleUrl;
        this.bundleDigest = "";
    }

    public BundleReader(DownloadedBundle downloadedBundle, EntandoDeBundle entandoDeBundle) {
        this.bundleBasePath = downloadedBundle.getLocalBundlePath();
        this.bundleDigest = downloadedBundle.getBundleDigest();
        this.entandoDeBundle = entandoDeBundle;
    }

    public BundleDescriptor readBundleDescriptor() throws IOException {
        return readBundleDescriptor(null);
    }

    public BundleDescriptor readBundleDescriptor(BundleDescriptorValidator bundleValidator) throws IOException {
        if (this.bundleDescriptor == null) {
            // read and assign
            this.bundleDescriptor = readDescriptorFile(BundleProperty.DESCRIPTOR_FILENAME.getValue(),
                    BundleDescriptor.class);

            // validate the bundle
            if (bundleValidator != null) {
                bundleValidator.validateOrThrow(bundleDescriptor);
            }

            this.bundleName = (!bundleDescriptor.isVersion1() && bundleDescriptor.isVersionEqualOrGreaterThan(
                    DescriptorVersion.V5))
                    ? bundleDescriptor.getName()
                    : bundleDescriptor.getCode();
            bundleDescriptor.setName(this.bundleName);

            // ensure the right code is used
            final String code = BundleUtilities.composeDescriptorCode(bundleDescriptor.getCode(),
                    bundleDescriptor.getName(), bundleDescriptor, getBundleUrl());
            bundleDescriptor.setCode(code);
        }

        return this.bundleDescriptor;
    }


    public boolean containsBundleResourceFolder() {
        return bundleBasePath.resolve(BundleProperty.RESOURCES_FOLDER_PATH.getValue()).toFile().isDirectory();
    }

    public boolean containsWidgetFolder() {
        return bundleBasePath.resolve(BundleProperty.WIDGET_FOLDER_PATH.getValue()).toFile().isDirectory();
    }

    public String getCode() throws IOException {
        return readBundleDescriptor().getCode();
    }

    public List<String> getResourceFolders() {
        return getResourceOfType(BundleProperty.RESOURCES_FOLDER_PATH.getValue(), Files::isDirectory);
    }

    public List<String> getResourceFiles() {
        return getResourceOfType(BundleProperty.RESOURCES_FOLDER_PATH.getValue(), Files::isRegularFile);
    }

    /**
     * create the list of each directory (at every level) present inside the widgets folder.
     *
     * @return the list of each directory (at every level) present inside the widgets folder
     */
    public List<String> getWidgetsFolders() {
        return getResourceOfType(BundleProperty.WIDGET_FOLDER_PATH.getValue(), Files::isDirectory);
    }

    /**
     * create the list of each directory directly descendant of the widgets folder.
     *
     * @return the list of each directory directly descendant of the widgets folder
     */
    public List<String> getWidgetsBaseFolders() throws IOException {
        Path widgetsPath = bundleBasePath.resolve(BundleProperty.WIDGET_FOLDER_PATH.getValue());

        try (Stream<Path> foldersStream = Files.list(widgetsPath)) {
            return foldersStream.filter(Files::isDirectory)
                    .map(Path::getFileName)
                    .map(f -> Paths.get(BundleProperty.WIDGET_FOLDER_PATH.getValue(), f.toString()))
                    .map(Path::toString)
                    .sorted(Comparator.naturalOrder())
                    .collect(Collectors.toList());
        }
    }

    public List<String> getWidgetsFiles() {
        var res = new ArrayList<String>();
        Path resourcePath = bundleBasePath.resolve(BundleProperty.WIDGET_FOLDER_PATH.getValue());
        if (resourcePath.toFile().exists()) {
            try (Stream<Path> paths = Files.walk(resourcePath, 1)) {
                paths.forEach(path -> {
                    if (Files.isDirectory(path) && !path.equals(resourcePath)) {
                        res.addAll(getResourceOfType(path.toString(), Files::isRegularFile));
                    }
                });
            } catch (IOException e) {
                log.error("Collection of widget files interrupted due to IO error", e);
            }
        } else {
            log.debug("Widgets directory:'{}' doesn't exist", resourcePath.toAbsolutePath());
        }
        return res;
    }

    public List<String> getResourceOfType(String resourcesPath, Predicate<Path> checkFunction) {
        List<Path> resources;
        Path resourcePath = bundleBasePath.resolve(resourcesPath);
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
        } catch (IOException e) {
            throw new EntandoComponentManagerException(String.format("Error reading descriptor file %s", fileName), e);
        }
    }

    private <T> T readDescriptorFile(final InputStream file, Class<T> clazz) throws IOException {
        return mapper.readValue(file, clazz);
    }

    public <T> List<T> readListOfDescriptorFile(final String filename, final Class<T> clazz) throws IOException {
        try (InputStream fis = new FileInputStream(bundleBasePath.resolve(filename).toFile())) {
            return readListOfDescriptorsFile(fis, clazz);
        }
    }

    private <T> List<T> readListOfDescriptorsFile(InputStream file, Class<T> clz) throws IOException {
        JavaType type = mapper.getTypeFactory().constructCollectionType(List.class, clz);
        return mapper.readValue(file, type);
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

    /**
     * returns the list of resources of the desired type located in the received folder (and subfolders).
     *
     * @param widgetFolder the folder in which search the desired resources
     * @param fileExt      the extension of the desired files
     * @return the list of the resources corresponding to the received search criteria
     */
    public List<String> getWidgetResourcesOfType(String widgetFolder, String fileExt) {
        return getResourceOfType(widgetFolder,
                (Path file) -> FilenameUtils.getExtension(file.toString()).equals(fileExt));
    }

    private void verifyFileExistance(String fileName) {
        log.debug("Reading file {}", fileName);
        if (!bundleBasePath.resolve(fileName).toFile().exists()) {
            throw new InvalidBundleException(String.format("File with name %s not found in the bundle", fileName));
        }
    }

    public File getAssetFile(final String directory, final String fileName) {
        return bundleBasePath.resolve(directory + "/" + fileName).toFile();
    }

    public String getDeBundleMetadataName() {
        if (this.entandoDeBundle == null) {
            throw new EntandoComponentManagerException("null entandoDeBundle detected while determining the bundle ID");
        }

        return this.entandoDeBundle.getMetadata().getName();
    }

    public String getBundleUrl() {
        if (ObjectUtils.isEmpty(this.bundleUrl)) {

            if (this.entandoDeBundle == null || this.entandoDeBundle.getSpec() == null
                    || ObjectUtils.isEmpty(this.entandoDeBundle.getSpec().getTags())
                    || this.entandoDeBundle.getSpec().getTags().get(0) == null
                    || ObjectUtils.isEmpty(this.entandoDeBundle.getSpec().getTags().get(0).getTarball())) {
                throw new EntandoComponentManagerException("cannot determine the bundle URL");
            }

            this.bundleUrl = this.entandoDeBundle.getSpec().getTags().get(0).getTarball();
        }

        return this.bundleUrl;
    }

    public boolean isBundleV1() {
        try {
            return this.readBundleDescriptor().isVersion1();
        } catch (IOException e) {
            throw new EntandoComponentManagerException("An error occurred while reading the bundle descriptor");
        }
    }

    public String calculateBundleId() {
        return BundleUtilities.removeProtocolAndGetBundleId(getBundleUrl());
    }

    public String getBundleName() {
        return bundleName;
    }

    public String getBundleDigest() {
        return bundleDigest;
    }
}
