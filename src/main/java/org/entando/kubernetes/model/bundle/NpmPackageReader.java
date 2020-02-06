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
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.entando.kubernetes.model.bundle.descriptor.ComponentDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.FileDescriptor;

public class NpmPackageReader {

    public static final String PACKAGE_ROOT = "package/";

    private final YAMLMapper mapper = new YAMLMapper();
    private final Map<String, File> tarEntries;
    private final TarArchiveInputStream tarInputStream;
    private final String RESOURCES_FOLDER_NAME = "resources";
    private final String RESOURCES_FOLDER_PATH = "resources/";

    public NpmPackageReader(Path filePath) throws IOException {
        this.tarInputStream = getGzipTarInputStream(filePath);
        this.tarEntries = buildTarEntries(this.tarInputStream, filePath);
    }

    public ComponentDescriptor readComponentDescriptor() throws IOException {
        return readDescriptorFile("descriptor.yaml", ComponentDescriptor.class);
    }

    private Map<String, File> buildTarEntries(TarArchiveInputStream i, Path tarPath) throws IOException {
        TarArchiveEntry tae;
        Map<String, File> tarEntries = new HashMap<>();
        String packageName = FilenameUtils.getBaseName(tarPath.getFileName().toString());
        while ( (tae = i.getNextTarEntry()) != null ) {
            if (!i.canReadEntryData(tae)) {
                // log something?
                continue;
            }
            File tmpf = File.createTempFile(tae.getName(), "." + packageName);
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
            tarEntries.put(tae.getName().replaceFirst(PACKAGE_ROOT, ""), tmpf);
        }
        return tarEntries;
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
                .anyMatch(n -> n.startsWith(RESOURCES_FOLDER_PATH));
    }

    public List<String> getResourceFolders() {
        return tarEntries.keySet().stream()
                .filter(path -> path.startsWith(RESOURCES_FOLDER_NAME))
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
                .filter(path -> path.startsWith(RESOURCES_FOLDER_NAME))
                .filter(path -> !tarEntries.get(path).isDirectory())
                .collect(Collectors.toList());
    }

    public <T> T readDescriptorFile(final String fileName, final Class<T> clazz) throws IOException {
        InputStream fis = new FileInputStream(tarEntries.get(fileName));
        return readDescriptorFile(fis, clazz);
    }

    public String readFileAsString(String fileName) throws IOException {
        InputStream fis = new FileInputStream(tarEntries.get(fileName));
        try (StringWriter writer = new StringWriter()) {
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
            final String folder = fileName.lastIndexOf('/') >= RESOURCES_FOLDER_PATH.length()
                    ? fileName.substring("resources/".length(), fileName.lastIndexOf('/'))
                    : "";
            return new FileDescriptor(folder, filename, base64);
        }
    }

    private <T> T readDescriptorFile(final InputStream file, Class<T> clazz) throws IOException {
        return mapper.readValue(file, clazz);
    }

}
