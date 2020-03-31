package org.entando.kubernetes.model.bundle.downloader;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.entando.kubernetes.model.debundle.EntandoDeBundleTag;

public class GitBundleDownloader extends BundleDownloader{

    @Override
    public Path saveBundleLocally(EntandoDeBundleTag tag) {
        try {
            if (targetPath == null) {
                this.createTargetDirectory();
            }
            Git.cloneRepository()
                    .setURI(tag.getTarball())
                    .setBranch(tag.getVersion())
                    .setCloneAllBranches(false)
                    .setBranchesToClone(Collections.singletonList("refs/tags/" + tag.getVersion()))
                    .setDirectory(targetPath.toFile())
                    .call();
            return targetPath;
        } catch (GitAPIException | IOException e) {
            throw new BundleDownloaderException("An error occurred while cloning git repo", e);
        }
    }
}
