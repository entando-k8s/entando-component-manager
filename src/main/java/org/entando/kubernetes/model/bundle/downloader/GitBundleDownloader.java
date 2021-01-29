package org.entando.kubernetes.model.bundle.downloader;

import static java.util.Optional.ofNullable;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.SystemUtils;
import org.entando.kubernetes.model.debundle.EntandoDeBundleTag;

public class GitBundleDownloader extends BundleDownloader {

    @Override
    protected Path saveBundleStrategy(EntandoDeBundleTag tag, Path targetPath) {
        try {
            //Ampie: removed HTTP validation because we support SSH now
            cloneUsingCliImplementation(tag, targetPath);
            return targetPath;
        } catch (IOException e) {
            throw new BundleDownloaderException("An error occurred while cloning git repo", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BundleDownloaderException("An error occurred while cloning git repo", e);
        }
    }

    private void cloneUsingCliImplementation(EntandoDeBundleTag tag, Path targetPath)
            throws IOException, InterruptedException {
        List<String> commands = new ArrayList<>();
        String gitCommand = String.format("git clone --branch %s --depth 1 %s %s",
                tag.getVersion(),
                tag.getTarball(),
                targetPath.toAbsolutePath());
        if (SystemUtils.IS_OS_WINDOWS) {
            commands.add("CMD");
            commands.add("/C");
        } else {
            commands.add("/bin/sh");
            commands.add("-c");
        }
        commands.add(gitCommand);

        ProcessBuilder pb = new ProcessBuilder(commands);
        pb.inheritIO();
        //Propagate LD_PRELOAD to ensure SSH can work
        ofNullable(System.getenv("LD_PRELOAD")).ifPresent((s) -> pb.environment().put("LD_PRELOAD", s));
        Process process = pb.start();

        //EDIT:
        // get Exit Status
        if (process.waitFor() != 0) {
            throw new BundleDownloaderException("An error occurred while shallow cloning the git repo");
        }
    }

}
