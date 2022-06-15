package org.entando.kubernetes.model.bundle.downloader;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import net.minidev.json.JSONObject;
import net.minidev.json.parser.JSONParser;
import org.apache.commons.lang3.StringUtils;
import org.entando.kubernetes.model.bundle.BundleProperty;
import org.entando.kubernetes.model.bundle.downloader.BundleDownloaderType.BundleDownloaderConstants;
import org.entando.kubernetes.model.debundle.EntandoDeBundleTag;
import org.entando.kubernetes.validator.ImageValidator;

@Slf4j
public class DockerBundleDownloader extends BundleDownloader {

    private static final String ERROR_WHILE_DOWNLOADING_IMAGE = "An error occurred while downloading image";
    private static final String ERROR_WHILE_FETCHING_TAGS_IMAGE = "An error occurred while fetching tag list";
    private static final String LOG_CMD_TO_EXECUTE = "Command to execute:'{}' with param to execute:'{}' and execution timeout:'{}'";
    private static final String IMAGE_LAYER_DIR = "image_dir";
    private static final String SKOPEO_CMD = "skopeo";
    private static final String TAR_CMD_PATH = "/usr/bin/tar";

    private final int downloadTimeoutSeconds;
    private final int downloadRetries;
    private final int decompressTimeoutSeconds;
    private static final ObjectMapper objectMapper = new ObjectMapper().configure(
            DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    private final String jsonContainerRegistryCredentials;
    private static final String ENV_NAME_ENTANDO_CONTAINER_REGISTRY_CREDENTIALS = "ENTANDO_CONTAINER_REGISTRY_CREDENTIALS";

    public DockerBundleDownloader(int downloadTimeoutSeconds, int downloadRetries, int decompressTimeoutSeconds) {
        this.downloadTimeoutSeconds = downloadTimeoutSeconds;
        this.downloadRetries = downloadRetries;
        this.decompressTimeoutSeconds = decompressTimeoutSeconds;
        this.jsonContainerRegistryCredentials = System.getenv(ENV_NAME_ENTANDO_CONTAINER_REGISTRY_CREDENTIALS);
    }

    /**
     * This method kill teh pending skopeo process and the pending tar process and creates a temporary directory.
     *
     * @return the Path to the newly created temporary directory
     * @throws IOException if an I/O error occurs
     */
    @Override
    public Path createTargetDirectory() throws IOException {
        // check pending process
        killPendingProcess();

        // create dir
        return super.createTargetDirectory();
    }

    /**
     * This method kill teh pending skopeo process and the pending tar process and deletes the temporary directory.
     */
    @Override
    public void cleanTargetDirectory() {
        // check pending process
        killPendingProcess();

        // delete dir
        super.cleanTargetDirectory();
    }

    /**
     * This method validate the url contained in the tarball field within the input tag and uses it to save the bundle
     * files to the target path. It uses: downloadTimeoutSeconds for the download process, downloadRetries as skopeo
     * parameter only for transient error like networking errors and decompressTimeoutSeconds for the tar process.
     *
     * @param tag        the tag with the tarball field that contains the fully qualified image url, i.e.:
     *                   docker://docker.io/nginx:8.0.1
     * @param targetPath the Path of the temporary directory to use to save the bundle files
     * @return the Path of the temporary directory used to save the bundle files
     */
    @Override
    protected Path saveBundleStrategy(EntandoDeBundleTag tag, Path targetPath) {
        log.info("Docker saveBundleStrategy");
        try {
            String fullyQualifiedImageUrl = generateFullyQualifiedWithTag(tag);
            ImageValidator.parse(fullyQualifiedImageUrl).isValidOrThrow(ERROR_WHILE_DOWNLOADING_IMAGE);

            saveContainerImage(fullyQualifiedImageUrl, targetPath);
            log.info("Docker image saved");
            untarContainerImage(targetPath);
            return targetPath;
        } catch (IOException e) {
            throw new BundleDownloaderException(ERROR_WHILE_DOWNLOADING_IMAGE, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BundleDownloaderException(ERROR_WHILE_DOWNLOADING_IMAGE, e);
        }
    }

    /**
     * This method validates the input url and uses it to save the bundle files to the target path. It uses:
     * downloadTimeoutSeconds for the download process, downloadRetries as skopeo parameter only for transient error
     * like networking errors and decompressTimeoutSeconds for the tar process.
     *
     * @param url        the fully qualified image url, i.e.: docker://docker.io/nginx:8.0.1
     * @param targetPath the Path of the temporary directory to use to save the bundle files
     * @return the Path of the temporary directory used to save the bundle files
     */
    @Override
    protected Path saveBundleStrategy(String url, Path targetPath) {
        try {
            ImageValidator.parse(url).isValidOrThrow(ERROR_WHILE_DOWNLOADING_IMAGE);

            saveContainerImage(url, targetPath);
            log.info("Docker image saved from url:'{}'", url);
            untarContainerImage(targetPath);
            return targetPath;
        } catch (IOException e) {
            throw new BundleDownloaderException(ERROR_WHILE_DOWNLOADING_IMAGE, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BundleDownloaderException(ERROR_WHILE_DOWNLOADING_IMAGE, e);
        }
    }


    /**
     * This method fetches the bundle tags as docker image tags. It uses downloadTimeoutSeconds for the download process
     * and used downloadRetries as skopeo parameter only for transient error like networking errors.
     *
     * @param repoUrl the fully qualified image url, i.e.: docker://docker.io/nginx:8.0.1
     * @return a list with found tags, the list can be empty
     */
    @Override
    public List<String> fetchRemoteTags(String repoUrl) {
        log.info("Fetching tags for the fully qualified image:'{}'", repoUrl);

        ImageValidator image = ImageValidator.parse(repoUrl);
        image.isValidOrThrow(ERROR_WHILE_FETCHING_TAGS_IMAGE);

        List<String> params = new ArrayList<>();
        params.add("list-tags");
        if (useCredentials()) {
            getCredentials(repoUrl).ifPresent(credentials -> {
                params.add("--creds");
                params.add(String.format("%s:%s", credentials.getUsername(), credentials.getPassword()));
            });
        }
        params.add("--retry-times");
        params.add("" + downloadRetries);

        String fullyQualifiedImageUrlWithoutTag = BundleDownloaderConstants.DOCKER_PROTOCOL
                + image.composeCommonUrlOrThrow(ERROR_WHILE_FETCHING_TAGS_IMAGE);
        params.add(fullyQualifiedImageUrlWithoutTag);

        log.info(LOG_CMD_TO_EXECUTE, SKOPEO_CMD, params.stream().collect(Collectors.joining(" ")),
                downloadTimeoutSeconds);

        try {
            ProcessHandler process = ProcessHandlerBuilder.buildCommand(SKOPEO_CMD, params, false).start()
                    .waitFor(downloadTimeoutSeconds);
            if (process.exitValue() == 0) {
                String body = process.getOutputLines().stream()
                        .collect(Collectors.joining("\n"));
                SkopeoTagList tagList = objectMapper.readValue(body, SkopeoTagList.class);
                log.debug("found tags info:'{}'", tagList);
                return new ArrayList<>(List.of(tagList.getTags()));

            } else {
                log.warn("Error fetching image tags, status:'{}'", process.exitValue());
                throw new BundleDownloaderException(ERROR_WHILE_FETCHING_TAGS_IMAGE);
            }
        } catch (IOException ex) {
            log.warn("Error retrieve tags", ex);
            throw new BundleDownloaderException(ERROR_WHILE_FETCHING_TAGS_IMAGE, ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            log.warn("Error retrieve tags", ex);
            throw new BundleDownloaderException(ERROR_WHILE_FETCHING_TAGS_IMAGE, ex);
        }

    }

    private void saveContainerImage(String fullyQualifiedImageUrl, Path targetPath)
            throws IOException, InterruptedException {
        log.info("saveContainerImage to path:'{}'", targetPath);

        // skopeo copy --src-no-creds --retry-times %s  %s dir:%s
        String param = "copy --retry-times";
        final Path tmpImage = Paths.get(targetPath.toString(), IMAGE_LAYER_DIR);
        List<String> params = new ArrayList<>(List.of(param.trim().split("\\s+")));
        params.add("" + downloadRetries);
        if (useCredentials()) {
            getCredentials(fullyQualifiedImageUrl).ifPresent(credentials -> {
                params.add("--src-creds");
                params.add(String.format("%s:%s", credentials.getUsername(), credentials.getPassword()));
            });
        }
        params.add(fullyQualifiedImageUrl);
        params.add("dir:" + tmpImage.toAbsolutePath());

        log.info(LOG_CMD_TO_EXECUTE, SKOPEO_CMD, params.stream().collect(Collectors.joining(" ")),
                downloadTimeoutSeconds);

        ProcessHandler processHandler = ProcessHandlerBuilder.buildCommand(SKOPEO_CMD, params)
                .start()
                .waitFor(downloadTimeoutSeconds);
        int exitStatus = processHandler.exitValue();
        if (exitStatus != 0 || !tmpImage.toFile().exists()) {
            log.warn("Error downloading image status:'{}' folder exists:'{}'", exitStatus, tmpImage.toFile().exists());
            throw new BundleDownloaderException(ERROR_WHILE_DOWNLOADING_IMAGE);
        } else {
            log.info("Ok download image, status:'{}'", exitStatus);
        }
    }

    private void untarContainerImage(Path targetPath) throws IOException, InterruptedException {
        Path tmpImageDir = Paths.get(targetPath.toString(), IMAGE_LAYER_DIR);

        Path imageManifest = Paths.get(tmpImageDir.toString(), "manifest.json");
        if (imageManifest.toFile().exists()) {

            Manifest manifest = objectMapper.readValue(imageManifest.toFile(), Manifest.class);
            for (Layer layer : manifest.getLayers()) {
                Path layerTarFile = Paths.get(tmpImageDir.toString(),
                        StringUtils.substringAfter(layer.getDigest(), ":"));
                if (layerTarFile.toFile().exists()) {
                    List<String> params = new ArrayList<>();
                    params.add("-xf");
                    params.add(layerTarFile.toAbsolutePath().toString());
                    params.add("-C");
                    params.add(targetPath.toAbsolutePath().toString());

                    log.info(LOG_CMD_TO_EXECUTE, TAR_CMD_PATH, params.stream().collect(Collectors.joining(" ")),
                            decompressTimeoutSeconds);

                    ProcessHandler processHandler = ProcessHandlerBuilder.buildCommand(TAR_CMD_PATH, params);
                    int exitStatus = processHandler.start()
                            .waitFor(decompressTimeoutSeconds).exitValue();
                    if (exitStatus != 0) {
                        log.warn("error unzip image status:'{}'", exitStatus);
                        throw new BundleDownloaderException(ERROR_WHILE_DOWNLOADING_IMAGE);
                    } else {
                        log.info("Ok unzip image, status:'{}'", exitStatus);
                    }
                } else {
                    log.warn("error for image layer with digest:'{}' path:'{}' not exist", layer.getDigest(),
                            layerTarFile.toAbsolutePath());
                    throw new BundleDownloaderException(ERROR_WHILE_DOWNLOADING_IMAGE);
                }
            }
            if (!descriptorExists(targetPath)) {
                log.warn("descriptor not exists:'{}'", descriptorExists(targetPath));
                throw new BundleDownloaderException(ERROR_WHILE_DOWNLOADING_IMAGE);
            }
        } else {
            log.warn("error image manifest path:'{}' not exist", imageManifest.toAbsolutePath());
            throw new BundleDownloaderException(ERROR_WHILE_DOWNLOADING_IMAGE);
        }
    }

    private boolean descriptorExists(Path dir) {
        Path descriptor = Paths.get(dir.toAbsolutePath().toString(), BundleProperty.DESCRIPTOR_FILENAME.getValue());
        return descriptor.toFile().exists();
    }

    private void killPendingProcess() {
        ProcessHandlerUtils.killProcessContainsName(SKOPEO_CMD);
        ProcessHandlerUtils.killProcessStartWithName(TAR_CMD_PATH);
    }

    private boolean useCredentials() {
        boolean useCredentials = StringUtils.isNotBlank(jsonContainerRegistryCredentials);
        log.debug("Should i use credentials? '{}'", useCredentials);
        return useCredentials;
    }

    private Optional<RegistryCredentials> getCredentials(String fullyQualifiedImageUrl) {
        if (StringUtils.isNotBlank(jsonContainerRegistryCredentials)) {
            ImageValidator image = ImageValidator.parse(fullyQualifiedImageUrl);
            log.info("manage jsonContainerRegistryCredentials:'{}' for domainRegistry:'{}'",
                    jsonContainerRegistryCredentials, image.getDomainRegistry());

            try {
                JSONObject rootObj = (JSONObject) new JSONParser(JSONParser.DEFAULT_PERMISSIVE_MODE).parse(
                        jsonContainerRegistryCredentials);
                JSONObject authObj = (JSONObject) rootObj.get("auths");
                JSONObject domainRegistryObj = (JSONObject) authObj.get(image.getDomainRegistry());
                if (domainRegistryObj == null) {
                    // WARNING I could use multiple registry one private other public
                    return Optional.empty();
                } else {
                    String username = (String) domainRegistryObj.get("username");
                    String password = (String) domainRegistryObj.get("password");

                    return Optional.ofNullable(new RegistryCredentials(username, password));
                }
            } catch (Exception ex) {
                log.error("Error retrieve registry credentials from env var name:'{}' value:'{}' for url:'{}'",
                        ENV_NAME_ENTANDO_CONTAINER_REGISTRY_CREDENTIALS, jsonContainerRegistryCredentials,
                        fullyQualifiedImageUrl, ex);
            }
        }
        throw new BundleDownloaderException(ERROR_WHILE_DOWNLOADING_IMAGE);
    }

    private String generateFullyQualifiedWithTag(EntandoDeBundleTag tag) {
        String fullyQualified = tag.getTarball();
        if (tag.getVersion() != null) {
            String sep = ":";
            if (StringUtils.startsWithIgnoreCase(tag.getVersion(), "sha256:")) {
                sep = "@";
            }
            fullyQualified = fullyQualified + sep + tag.getVersion();

        }
        return fullyQualified;
    }

    // support class used to easily unmarshalling json and to improve readability
    @Getter
    @Setter
    @AllArgsConstructor
    @ToString
    private static class RegistryCredentials {

        private String username;
        private String password;
    }

    @Getter
    @Setter
    private static class Manifest {

        private Layer[] layers;
    }

    @Getter
    @Setter
    private static class Layer {

        private String digest;
    }

    @Getter
    @Setter
    @ToString
    private static class SkopeoTagList {

        @JsonProperty("Tags")
        private String[] tags = new String[0];
    }

}
