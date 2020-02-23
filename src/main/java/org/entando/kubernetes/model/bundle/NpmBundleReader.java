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
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.entando.kubernetes.exception.digitalexchange.InvalidBundleException;
import org.entando.kubernetes.model.bundle.descriptor.ComponentDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.FileDescriptor;

public class NpmBundleReader{

    private final YAMLMapper mapper = new YAMLMapper();
    private final Map<String, File> tarEntries;
    private final TarArchiveInputStream tarInputStream;

    public NpmBundleReader(Path filePath) throws IOException {
        this.tarInputStream = getGzipTarInputStream(filePath);
        this.tarEntries = buildTarEntries(this.tarInputStream, filePath);
    }

    public ComponentDescriptor readBundleDescriptor() throws IOException {
        return readDescriptorFile(BundleProperty.DESCRIPTOR_FILENAME.getValue(), ComponentDescriptor.class);
    }

    private Map<String, File> buildTarEntries(TarArchiveInputStream i, Path tarPath) throws IOException {
        TarArchiveEntry tae;
        Map<String, File> tes = new HashMap<>();
        String packageName = FilenameUtils.getBaseName(tarPath.getFileName().toString());
        Path packageTempDir = Files.createTempDirectory(packageName);
        while ( (tae = i.getNextTarEntry()) != null ) {
            if (!i.canReadEntryData(tae)) {
                // log something?
                continue;
            }
            File tmpf = File.createTempFile(tae.getName(), "." + packageName, packageTempDir.toFile());
            tmpf.deleteOnExit();
            if (tae.isDirectory()) {
                if (!tmpf.isDirectory() && !tmpf.mkdirs()) {
                    throw new IOException("failed to create directory " + tmpf);
                }
            } else {
                File parent = tmpf.getParentFile();
                if (!parent.isDirectory() && !parent.mkdirs()) {
                    throw new IOException("failed to create directory " + parent);
                }
                try (OutputStream o = Files.newOutputStream(tmpf.toPath())) {
                    IOUtils.copy(i, o);
                }
            }
            tes.put(tae.getName(), tmpf);
        }
        return rebaseEntriesNamesOnComponentDescriptorFile(tes);
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

    private TarArchiveInputStream getGzipTarInputStream(Path p) {
        try {
            return new TarArchiveInputStream(new GzipCompressorInputStream(newInputStream(p, READ)));
        } catch (IOException e) {
            throw new UncheckedIOException("An error occurred while reading file " + p.getFileName().toString(), e);
        }
    }

    public Map<String, File> getTarEntries() {
        return this.tarEntries;
    }

    public void destroy() {
        this.tarEntries.values().forEach(File::delete);
    }

    public boolean containsResourceFolder() {
        return tarEntries.keySet().stream()
                .anyMatch(n -> n.startsWith(BundleProperty.RESOURCES_FOLDER_PATH.getValue()));
    }

    public List<String> getResourceFolders() {
        return tarEntries.keySet().stream()
                .filter(path -> path.startsWith(BundleProperty.RESOURCES_FOLDER_NAME.getValue()))
                .map(FilenameUtils::getFullPath) // Not always directory entry is available as a single path in the zip
                .distinct()
                .filter(path ->
                        !path.equals(BundleProperty.RESOURCES_FOLDER_NAME.getValue()) &&
                        !path.equals(BundleProperty.RESOURCES_FOLDER_PATH.getValue()))
                .map(path -> path.substring(BundleProperty.RESOURCES_FOLDER_PATH.getValue().length(), path.length() - 1))
                .flatMap(this::getIntermediateFolders)
                .distinct()
                .sorted(Comparator.comparing(String::length))
                .collect(Collectors.toList());
    }

    /**
     * Given a directory, returns a stream of all the intermediate directories e.g. /static/img/svg/full-size ->
     * {static, static/img, static/img/svg, static/img/svg/full-size}
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
        return tarEntries.keySet().stream()
                .filter(path -> path.startsWith(BundleProperty.RESOURCES_FOLDER_NAME.getValue()))
                .filter(path -> !tarEntries.get(path).isDirectory())
                .collect(Collectors.toList());
    }

    public <T> T readDescriptorFile(final String fileName, final Class<T> clazz) throws IOException {
        InputStream fis = new FileInputStream(tarEntries.get(fileName));
        return readDescriptorFile(fis, clazz);
    }

    public String readFileAsString(String fileName) throws IOException {
        try (InputStream fis = new FileInputStream(tarEntries.get(fileName));
             StringWriter writer = new StringWriter()) {
            IOUtils.copy(fis, writer, StandardCharsets.UTF_8);
            return writer.toString();
        }
    }

    public FileDescriptor readFileAsDescriptor(final String fileName) throws IOException {
        File f = tarEntries.get(fileName);
        try (final ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            IOUtils.copy(new FileInputStream(f), outputStream);
            final String base64 = Base64.encodeBase64String(outputStream.toByteArray());
            final String filename = fileName.substring(fileName.lastIndexOf('/') + 1);
            final String folder = fileName.lastIndexOf('/') >= BundleProperty.RESOURCES_FOLDER_PATH.getValue().length()
                    ? fileName.substring("resources/".length(), fileName.lastIndexOf('/'))
                    : "";
            return new FileDescriptor(folder, filename, base64);
        }
    }

    private <T> T readDescriptorFile(final InputStream file, Class<T> clazz) throws IOException {
        return mapper.readValue(file, clazz);
    }

}
