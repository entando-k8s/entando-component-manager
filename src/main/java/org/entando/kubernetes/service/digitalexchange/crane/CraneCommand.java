package org.entando.kubernetes.service.digitalexchange.crane;

// TODO refine this class to design a real command pattern

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.entando.kubernetes.exception.EntandoComponentManagerException;
import org.entando.kubernetes.model.bundle.downloader.ProcessHandler;
import org.entando.kubernetes.model.bundle.downloader.ProcessHandlerBuilder;
import org.entando.kubernetes.model.bundle.downloader.ProcessHandlerUtils;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class CraneCommand {

    public static final String CRANE_CMD = "crane";

    private static final String ERROR_GETTING_IMAGE_DIGEST =
            "An error occurred while fetching the docker image digest using " + CRANE_CMD;

    public String getImageDigest(String image) {

        ProcessHandlerUtils.killProcessContainsName(CRANE_CMD);

        List<String> params = Arrays.asList("digest", image);

        ProcessHandler processHandler = null;
        try {
            processHandler = ProcessHandlerBuilder.buildCommand(CRANE_CMD, params, false)
                    .start()
                    .waitFor(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn(ERROR_GETTING_IMAGE_DIGEST, e.getCause());
            throw new CraneException(ERROR_GETTING_IMAGE_DIGEST, e.getCause());
        } catch (Exception e) {
            log.warn(ERROR_GETTING_IMAGE_DIGEST, e);
            throw new CraneException(ERROR_GETTING_IMAGE_DIGEST, e);
        }

        int exitStatus = processHandler.exitValue();
        if (exitStatus != 0) {
            String err = String.format(ERROR_GETTING_IMAGE_DIGEST + " - exit status: '%s'", exitStatus);
            log.warn(err);
            throw new CraneException(err);
        }

        List<String> outputLines;
        try {
            outputLines = processHandler.getOutputLines();
        } catch (IOException e) {
            String err = String.format(ERROR_GETTING_IMAGE_DIGEST + " - error: '%s'", e.getMessage());
            throw new CraneException(err);
        }

        if (outputLines.size() != 1) {
            final String result = String.join("\n", outputLines);
            String err = String.format(ERROR_GETTING_IMAGE_DIGEST + " - unexpected output: '%s'", result);
            throw new CraneException(err);
        }

        log.info("Docker image: {} - Digest: {}", image, outputLines.get(0));

        return outputLines.get(0);
    }


    public static class CraneException extends EntandoComponentManagerException {

        public CraneException(String message) {
            super(message);
        }

        public CraneException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
