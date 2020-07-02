package org.entando.kubernetes.model.bundle.downloader;

import org.entando.kubernetes.model.debundle.EntandoDeBundleTag;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class GitBundleDownloader extends BundleDownloader {

    @Override
    protected Path saveBundleStrategy(EntandoDeBundleTag tag, Path targetPath) {
        try {
            validateRepoUrl(tag.getTarball());
            cloneUsingCliImplementation(tag, targetPath);
            return targetPath;
        } catch (IOException e) {
            throw new BundleDownloaderException("An error occurred while cloning git repo", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BundleDownloaderException("An error occurred while cloning git repo", e);
        }
    }

    private void validateRepoUrl(String tarball) {
        if (!tarball.matches("^https?://.*$")) {
            throw new BundleDownloaderException(
                    "Unsupported repository " + tarball + "; Only HTTP(s) repositories are supported");
        }
    }

    private void cloneUsingCliImplementation(EntandoDeBundleTag tag, Path targetPath)
            throws IOException, InterruptedException {
        List<String> commands = new ArrayList<>();
        String gitCommand = String.format("git clone --branch %s --depth 1 %s %s",
                tag.getVersion(),
                tag.getTarball(),
                targetPath.toAbsolutePath());
        commands.add("/bin/sh");
        commands.add("-c");
        commands.add(gitCommand);

        ProcessBuilder pb = new ProcessBuilder(commands);
        pb.redirectErrorStream(true);
        Process process = pb.start();

        //EDIT:
        // get Exit Status
        if (process.waitFor() != 0) {
            throw new BundleDownloaderException("An error occurred while shallow cloning the git repo");
        }
    }

}
