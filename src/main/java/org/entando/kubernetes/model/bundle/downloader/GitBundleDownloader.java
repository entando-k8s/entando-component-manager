package org.entando.kubernetes.model.bundle.downloader;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.entando.kubernetes.model.debundle.EntandoDeBundleTag;

public class GitBundleDownloader extends BundleDownloader{

    @Override
    protected Path saveBundleStrategy(EntandoDeBundleTag tag, Path targetPath) {
        try {
            cloneUsingCliImplementation(tag, targetPath);

//            cloneUsingJGitImplementation(tag, targetPath);
            return targetPath;
        } catch (IOException | InterruptedException e) {
            throw new BundleDownloaderException("An error occurred while cloning git repo", e);
        }
    }

    private void cloneUsingCliImplementation(EntandoDeBundleTag tag, Path targetPath)
            throws IOException, InterruptedException {
        List<String> commands = new ArrayList<>();
        String gitCommand = String.format( "git clone --branch %s --depth 1 %s %s",
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
        if ( process.waitFor() != 0)
            throw new BundleDownloaderException("An error occurred while shallow cloning the git repo");
    }

    private void cloneUsingJGitImplementation(EntandoDeBundleTag tag, Path targetPath) throws GitAPIException {
        Git.cloneRepository()
                .setURI(tag.getTarball())
                .setBranch(tag.getVersion())
                .setCloneAllBranches(false)
                .setBranchesToClone(Collections.singletonList("refs/tags/" + tag.getVersion()))
                .setDirectory(targetPath.toFile())
                .call();
    }
}
