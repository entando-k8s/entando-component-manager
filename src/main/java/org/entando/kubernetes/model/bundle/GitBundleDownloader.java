package org.entando.kubernetes.model.bundle;

import java.nio.file.Path;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.entando.kubernetes.model.debundle.EntandoDeBundleTag;

public class GitBundleDownloader implements BundleDownloader{

    @Override
    public void saveBundleLocally(EntandoDeBundleTag tag, Path destination) {
        try {
            Git repo = Git.cloneRepository()
                    .setURI(tag.getTarball())
                    .setBranch("master")
                    .setDirectory(destination.toFile())
                    .call();
            repo.checkout().setName("refs/tags/" + tag.getVersion()).call();
        } catch (GitAPIException e) {
            throw new BundleDownloaderException("An error occurred while cloning git repo", e);
        }
    }
}
