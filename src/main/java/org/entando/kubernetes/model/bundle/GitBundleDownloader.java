package org.entando.kubernetes.model.bundle;

import java.io.IOException;
import java.nio.file.Path;
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
            Git repo = Git.cloneRepository()
                    .setURI(tag.getTarball())
                    .setBranch("master")
                    .setDirectory(targetPath.toFile())
                    .call();
            repo.checkout().setName("refs/tags/" + tag.getVersion()).call();
            return targetPath;
        } catch (GitAPIException | IOException e) {
            throw new BundleDownloaderException("An error occurred while cloning git repo", e);
        }
    }
}
