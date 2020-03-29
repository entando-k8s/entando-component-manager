package org.entando.kubernetes.model.bundle;

import static java.nio.file.Files.newInputStream;
import static java.nio.file.StandardOpenOption.READ;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.entando.kubernetes.exception.job.JobPackageException;
import org.entando.kubernetes.model.debundle.EntandoDeBundleTag;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.client.RestTemplate;

public class NpmBundleDownloader implements BundleDownloader {

    @Override
    public Path saveBundleLocally(EntandoDeBundleTag tag, Path destination) throws BundleDownloaderException {
        InputStream is = downloadComponentPackage(tag);
        Path tarPath = savePackageStreamLocally(is);

        try {
            unpackTar(tarPath, destination);
        } catch (IOException e) {
            throw new BundleDownloaderException(e);
        }

        return destination;
    }

    private TarArchiveInputStream getGzipTarInputStream(Path p) {
        try {
            return new TarArchiveInputStream(new GzipCompressorInputStream(newInputStream(p, READ)));
        } catch (IOException e) {
            throw new UncheckedIOException("An error occurred while reading file " + p.getFileName().toString(), e);
        }
    }

    private Path unpackTar(Path tarPath, Path destination) throws IOException {

        TarArchiveInputStream tarInputStream = getGzipTarInputStream(tarPath);

        TarArchiveEntry tae;
        Map<String, File> tes = new HashMap<>();
        String packageName = FilenameUtils.getBaseName(tarPath.getFileName().toString());
        while ( (tae = tarInputStream.getNextTarEntry()) != null ) {
            if (!tarInputStream.canReadEntryData(tae)) {
                // log something?
                continue;
            }
            Path filePath = destination.resolve(tae.getName());
            // File tmpf = File.createTempFile(tae.getName(), "." + packageName, destination.toFile());
            File tmpf = filePath.toFile();
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
                    IOUtils.copy(tarInputStream, o);
                }
            }
            tes.put(tae.getName(), tmpf);
        }
        return destination;

    }


    private InputStream downloadComponentPackage(EntandoDeBundleTag tag) {
        String tarballUrl = tag.getTarball();

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.add("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/54.0.2840.99 Safari/537.36");
        headers.add("Accept", "*/*");
        HttpEntity<String> entity = new HttpEntity<>("parameters", headers);

        ResponseEntity<Resource> responseEntity =  restTemplate.exchange(
                tarballUrl, HttpMethod.GET, entity, Resource.class);

        if (responseEntity.getBody() == null) {
            throw new HttpMessageNotReadableException("Response body is null");
        }

        try {
            return responseEntity.getBody().getInputStream();
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }

    }

    private Path savePackageStreamLocally(InputStream is) {
        Path tempPath = null;
        try {
            tempPath = Files.createTempFile(null, null);
            Files.copy(is, tempPath, StandardCopyOption.REPLACE_EXISTING);
            return tempPath;
        } catch (IOException e) {
            throw new JobPackageException(tempPath,
                    "An error occurred while copying the package stream locally", e);
        }
    }
}
