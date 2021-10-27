package org.entando.kubernetes.model.bundle.downloader;

import static java.nio.file.Files.newInputStream;
import static java.nio.file.StandardOpenOption.READ;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.io.IOUtils;
import org.entando.kubernetes.exception.EntandoComponentManagerException;
import org.entando.kubernetes.exception.job.JobPackageException;
import org.entando.kubernetes.model.debundle.EntandoDeBundleTag;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

public class NpmBundleDownloader extends BundleDownloader {

    @Override
    protected Path saveBundleStrategy(EntandoDeBundleTag tag, Path targetPath) {
        try {
            InputStream is = downloadComponentPackage(tag);
            Path tarPath = savePackageStreamLocally(is);
            unpackTar(tarPath, targetPath);
            return targetPath;
        } catch (IOException e) {
            throw new BundleDownloaderException(e);
        }
    }

    @Override
    protected Path saveBundleStrategy(URL url, Path targetPath) {
        throw new EntandoComponentManagerException("Not yet implemented");
    }

    @Override
    public List<String> fetchRemoteTags(URL repoUrl) {
        throw new EntandoComponentManagerException("Not yet implemented");
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
        while ((tae = tarInputStream.getNextTarEntry()) != null) {
            if (!tarInputStream.canReadEntryData(tae)) {
                // log something?
                continue;
            }

            String rebasedName = tae.getName().replaceFirst("package/", "");

            Path filePath = destination.resolve(rebasedName);
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
        headers.add("user-agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/54.0.2840.99 Safari/537.36");
        headers.add("Accept", "*/*");
        HttpEntity<String> entity = new HttpEntity<>("parameters", headers);

        ResponseEntity<Resource> responseEntity = restTemplate.exchange(
                tarballUrl, HttpMethod.GET, entity, Resource.class);

        if (responseEntity.getBody() == null) {
            throw new BundleDownloaderException("Requested package returned an empty body");
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
