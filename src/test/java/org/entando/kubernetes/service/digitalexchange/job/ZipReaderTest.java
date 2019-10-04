package org.entando.kubernetes.service.digitalexchange.job;

import java.util.Arrays;
import org.entando.kubernetes.service.digitalexchange.job.model.FileDescriptor;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static org.apache.commons.codec.binary.Base64.encodeBase64String;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.same;
import static org.mockito.Mockito.when;

@SuppressWarnings("unchecked")
public class ZipReaderTest {

    private @Mock ZipFile zipFile;
    private ZipReader zipReader;

    @Before
    public void setUp() throws IOException {
        MockitoAnnotations.initMocks(this);

        final List<ZipEntry> entries = new ArrayList<>();
        entries.add(createFile("descriptor.yaml", "my descriptor file"));
        entries.add(createDirectory("resources/"));
        entries.add(createFile("resources/favicon.ico", "icon"));
        entries.add(createFile("resources/css/styles.css", "body { padding: 0 }"));
        entries.add(createFile("resources/js/script.js", "$(document).ready();"));
        entries.add(createFile("resources/static/js/script.js", "console.log(1)"));
        entries.add(createDirectory("resources/static/img/svg/"));

        final Stream stream = entries.stream();
        when(zipFile.stream()).thenReturn(stream);

        zipReader = new ZipReader(zipFile);
    }

    @Test
    public void testZipReader() throws IOException {
        assertThat(zipReader.containsResourceFolder()).isTrue();

        final FileDescriptor styles = zipReader.readFileAsDescriptor("resources/css/styles.css");
        assertThat(styles.getFilename()).isEqualTo("styles.css");
        assertThat(styles.getFolder()).isEqualTo("css");
        assertThat(styles.getBase64()).isEqualTo(encodeBase64String("body { padding: 0 }".getBytes()));

        final FileDescriptor script = zipReader.readFileAsDescriptor("resources/js/script.js");
        assertThat(script.getFilename()).isEqualTo("script.js");
        assertThat(script.getFolder()).isEqualTo("js");
        assertThat(script.getBase64()).isEqualTo(encodeBase64String("$(document).ready();".getBytes()));

        final FileDescriptor favicon = zipReader.readFileAsDescriptor("resources/favicon.ico");
        assertThat(favicon.getFilename()).isEqualTo("favicon.ico");
        assertThat(favicon.getFolder()).isEqualTo("");
        assertThat(favicon.getBase64()).isEqualTo(encodeBase64String("icon".getBytes()));
    }

    @Test(expected = FileNotFoundException.class)
    public void testFileNotFound() throws IOException {
        zipReader.readFileAsDescriptor("resources/notfound.ico");
    }

    @Test
    public void testGetResourceFolders() {
        List<String> resourceFolders = zipReader.getResourceFolders();
        List<String> expectedFolders = Arrays.asList("js", "css", "static", "static/js", "static/img", "static/img/svg");
        assertThat(resourceFolders).isEqualTo(expectedFolders);
    }

    private ZipEntry createFile(final String fileName, final String content) throws IOException {
        final ZipEntry entry = mock(ZipEntry.class);
        when(entry.getName()).thenReturn(fileName);
        when(zipFile.getInputStream(same(entry))).thenReturn(new ByteArrayInputStream(content.getBytes()));
        return entry;
    }

    private ZipEntry createDirectory(final String folderName) {
        final ZipEntry entry = mock(ZipEntry.class);
        when(entry.getName()).thenReturn(folderName);
        when(entry.isDirectory()).thenReturn(true);
        return entry;
    }

}
