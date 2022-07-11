package org.entando.kubernetes.model.bundle.downloader;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import net.minidev.json.JSONObject;
import net.minidev.json.parser.JSONParser;
import org.apache.commons.lang3.StringUtils;
import org.entando.kubernetes.model.bundle.BundleProperty;
import org.entando.kubernetes.model.debundle.EntandoDeBundleTag;
import org.entando.kubernetes.validator.ImageValidator;

@Slf4j
public class DockerBundleDownloader extends BundleDownloader {

    private static final String ERROR_WHILE_DOWNLOADING_IMAGE = "An error occurred while downloading image";
    private static final String ERROR_WHILE_FETCHING_TAGS_IMAGE = "An error occurred while fetching tag list";
    private static final String ERROR_WHILE_SETTING_REPOSITORY_CREDENTIALS = "An error occurred while setting image repository credentials";
    private static final String LOG_CMD_TO_EXECUTE = "Command to execute:'{}' with param to execute:'{}' and execution timeout:'{}'";
    private static final String IMAGE_TAR = "image.tar";
    private static final String CRANE_CMD = "crane";
    private static final String TAR_CMD_PATH = "/usr/bin/tar"; //NOSONAR

    private final int downloadTimeoutSeconds;
    private final int downloadRetries;
    private final int decompressTimeoutSeconds;
    private final String jsonContainerRegistryCredentials;

    public DockerBundleDownloader(int downloadTimeoutSeconds, int downloadRetries, int decompressTimeoutSeconds,
            String jsonContainerRegistryCredentials) {
        this.downloadTimeoutSeconds = downloadTimeoutSeconds;
        this.downloadRetries = downloadRetries;
        this.decompressTimeoutSeconds = decompressTimeoutSeconds;
        this.jsonContainerRegistryCredentials = jsonContainerRegistryCredentials;
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
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BundleDownloaderException(ERROR_WHILE_DOWNLOADING_IMAGE, e);
        } catch (Exception e) {
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
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BundleDownloaderException(ERROR_WHILE_DOWNLOADING_IMAGE, e);
        } catch (Exception e) {
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

        // crane ls repo
        List<String> params = new ArrayList<>();
        params.add("ls");
        if (useCredentials()) {
            getCredentials(repoUrl).ifPresent(this::doCraneAuthLogin);
        }
        String fullyQualifiedImageUrlWithoutTag = image.composeCommonUrlWithoutTransportWithoutTagOrThrow(
                ERROR_WHILE_FETCHING_TAGS_IMAGE);
        params.add(fullyQualifiedImageUrlWithoutTag);

        log.info("Executing crane tags list");
        log.debug(LOG_CMD_TO_EXECUTE, CRANE_CMD, params.stream().collect(Collectors.joining(" ")),
                downloadTimeoutSeconds);

        try {
            var methodRetryer = MethodRetryer.<ExecProcessInputParameters, ExecResult>builder()
                    .retries(downloadRetries)
                    .execMethod(this::genericExecuteCommand)
                    .checkerMethod(this::checker)
                    .build();

            ExecResult result = methodRetryer.execute(ExecProcessInputParameters.builder()
                    .command(CRANE_CMD)
                    .params(params)
                    .timeout(downloadTimeoutSeconds)
                    .inheritIO(false)
                    .build());

            if (result.getEx() != null) {
                throw result.getEx();
            }

            ProcessHandler process = result.getProcess();
            if (result.getProcess().exitValue() == 0) {
                List<String> tagList = process.getOutputLines().stream().collect(Collectors.toList());
                log.debug("found tags info:'{}'", tagList);
                return tagList;

            } else {
                log.warn("Error fetching image tags, status:'{}'", process.exitValue());
                throw new BundleDownloaderException(ERROR_WHILE_FETCHING_TAGS_IMAGE);
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            log.warn("Error retrieve tags", ex);
            throw new BundleDownloaderException(ERROR_WHILE_FETCHING_TAGS_IMAGE, ex);
        } catch (Exception ex) {
            log.warn("Error retrieve tags", ex);
            throw new BundleDownloaderException(ERROR_WHILE_FETCHING_TAGS_IMAGE, ex);
        }

    }

    private void saveContainerImage(String fullyQualifiedImageUrl, Path targetPath) throws Exception {
        log.info("saveContainerImage to path:'{}'", targetPath);

        // crane export docker.io/entando/ /targetPath/image.tar
        String param = "export";
        final Path tarImage = Paths.get(targetPath.toString(), IMAGE_TAR);
        List<String> params = new ArrayList<>(List.of(param.trim().split("\\s+")));
        if (useCredentials()) {
            getCredentials(fullyQualifiedImageUrl).ifPresent(this::doCraneAuthLogin);
        }

        ImageValidator image = ImageValidator.parse(fullyQualifiedImageUrl);
        image.isValidOrThrow(ERROR_WHILE_DOWNLOADING_IMAGE);
        String urlWithTagNoTransport = image.composeCommonUrlWithoutTransportOrThrow(
                ERROR_WHILE_FETCHING_TAGS_IMAGE);

        params.add(urlWithTagNoTransport);
        params.add(tarImage.toAbsolutePath().toString());

        log.info("Executing crane image download");
        log.debug(LOG_CMD_TO_EXECUTE, CRANE_CMD, params.stream().collect(Collectors.joining(" ")),
                downloadTimeoutSeconds);

        var methodRetryer = MethodRetryer.<ExecProcessInputParameters, ExecResult>builder()
                .retries(downloadRetries)
                .checkerMethod(this::checker)
                .execMethod(this::genericExecuteCommand).build();

        ExecResult result = methodRetryer.execute(ExecProcessInputParameters.builder()
                .command(CRANE_CMD)
                .params(params)
                .timeout(downloadTimeoutSeconds)
                .inheritIO(true)
                .build());

        if (result.getEx() != null) {
            throw result.getEx();
        }
        int exitStatus = result.getReturnValue();
        if (exitStatus != 0 || !tarImage.toFile().exists()) {
            log.warn("Error downloading image status:'{}' tarImage exists:'{}'", exitStatus,
                    tarImage.toFile().exists());
            throw new BundleDownloaderException(ERROR_WHILE_DOWNLOADING_IMAGE);
        } else {
            log.info("Ok download image, status:'{}'", exitStatus);
        }
    }

    private void untarContainerImage(Path targetPath) throws Exception {
        Path tarImagePath = Paths.get(targetPath.toString(), IMAGE_TAR);

        if (tarImagePath.toFile().exists()) {
            List<String> params = new ArrayList<>();
            params.add("-xf");
            params.add(tarImagePath.toAbsolutePath().toString());
            params.add("-C");
            params.add(targetPath.toAbsolutePath().toString());

            log.info("Executing decompress of the image downloaded");
            log.debug(LOG_CMD_TO_EXECUTE, TAR_CMD_PATH, params.stream().collect(Collectors.joining(" ")),
                    decompressTimeoutSeconds);

            var methodRetryer = MethodRetryer.<ExecProcessInputParameters, ExecResult>builder()
                    .retries(1).execMethod(this::genericExecuteCommand)
                    .checkerMethod(this::checker)
                    .build();

            ExecResult result = methodRetryer.execute(
                    ExecProcessInputParameters.builder()
                            .command(TAR_CMD_PATH).params(params)
                            .timeout(decompressTimeoutSeconds)
                            .inheritIO(true)
                            .build());

            if (result.getEx() != null) {
                throw result.getEx();
            }
            int exitStatus = result.getReturnValue();

            if (exitStatus != 0) {
                log.warn("error decompress image process status:'{}'", exitStatus);
                throw new BundleDownloaderException(ERROR_WHILE_DOWNLOADING_IMAGE);
            } else {
                log.debug("Ok decompress image, status:'{}'", exitStatus);
                if (!descriptorExists(targetPath)) {
                    log.warn("descriptor not exists:'{}'", descriptorExists(targetPath));
                    throw new BundleDownloaderException(ERROR_WHILE_DOWNLOADING_IMAGE);
                }
            }
        } else {
            log.warn("error tar file from crane with path:'{}' not exist", tarImagePath.toAbsolutePath());
            throw new BundleDownloaderException(ERROR_WHILE_DOWNLOADING_IMAGE);
        }
    }

    private boolean checker(ExecResult result) {
        boolean success = result.getReturnValue() == 0;
        boolean isException = result.getEx() != null;
        return success || isException;
    }

    private ExecResult genericExecuteCommand(ExecProcessInputParameters parameters) {
        ExecResult result = new ExecResult();
        try {

            ProcessHandler processHandler = ProcessHandlerBuilder.buildCommand(parameters.getCommand(),
                            parameters.getParams(), parameters.isInheritIO()).start()
                    .waitFor(parameters.getTimeout());
            int exitStatus = processHandler.exitValue();

            result.setReturnValue(exitStatus);
            result.setProcess(processHandler);

        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            result.setEx(ex);

        } catch (IOException ex) {
            result.setEx(ex);
        }
        return result;
    }


    private void doCraneAuthLogin(RegistryCredentials credentials) {
        log.info("doCraneAuthLogin for domain registry:'{}'", credentials.getDomainRegistry());

        // crane auth login -u username -p password domainRegistry
        List<String> params = composeCraneAuthParamsList(credentials, credentials.getPassword());

        log.info("Executing crane repository credentials load");
        log.debug(LOG_CMD_TO_EXECUTE, CRANE_CMD, composeCraneAuthParamsList(credentials, "******").stream()
                        .collect(Collectors.joining(" ")),
                downloadTimeoutSeconds);

        ProcessHandler processHandler = null;
        try {
            processHandler = ProcessHandlerBuilder.buildCommand(CRANE_CMD, params)
                    .start()
                    .waitFor(downloadTimeoutSeconds);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            log.warn("Error executing cran auth login", ex);
            throw new BundleDownloaderException(ERROR_WHILE_SETTING_REPOSITORY_CREDENTIALS, ex);
        } catch (IOException ex) {
            log.error("Error executing cran auth login", ex);
            throw new BundleDownloaderException(ERROR_WHILE_SETTING_REPOSITORY_CREDENTIALS, ex);
        }

        int exitStatus = processHandler.exitValue();
        if (exitStatus != 0) {
            log.warn("Error doing cran auth login, status:'{}'", exitStatus);
            throw new BundleDownloaderException(ERROR_WHILE_DOWNLOADING_IMAGE);
        } else {
            log.info("Ok doing cran auth login, status:'{}'", exitStatus);
        }
    }

    private List<String> composeCraneAuthParamsList(RegistryCredentials credentials, String password) {
        String param = "auth login";
        List<String> params = new ArrayList<>(List.of(param.trim().split("\\s+")));
        params.add("-u");
        params.add(credentials.getUsername());
        params.add("-p");
        params.add(password);
        params.add(credentials.getDomainRegistry());
        return params;
    }

    private boolean descriptorExists(Path dir) {
        Path descriptor = Paths.get(dir.toAbsolutePath().toString(), BundleProperty.DESCRIPTOR_FILENAME.getValue());
        return descriptor.toFile().exists();
    }

    private void killPendingProcess() {
        ProcessHandlerUtils.killProcessContainsName(CRANE_CMD);
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
            log.debug("manage jsonContainerRegistryCredentials for domainRegistry:'{}'", image.getDomainRegistry());

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

                    return Optional.ofNullable(new RegistryCredentials(image.getDomainRegistry(), username, password));
                }
            } catch (Exception ex) {
                log.error("Error retrieve registry credentials from env value:'{}' for url:'{}'",
                        jsonContainerRegistryCredentials,
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


    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @ToString
    public static class ExecResult {

        private int returnValue;
        private Exception ex;
        private ProcessHandler process;

    }

    // support class used to easily unmarshalling json and to improve readability
    @Getter
    @Setter
    @AllArgsConstructor
    @ToString
    private static class RegistryCredentials {

        private String domainRegistry;
        private String username;
        private String password;
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @Builder
    @ToString
    private static class ExecProcessInputParameters {

        private final String command;
        private final List<String> params;
        private final int timeout;
        private boolean inheritIO;
    }
}
