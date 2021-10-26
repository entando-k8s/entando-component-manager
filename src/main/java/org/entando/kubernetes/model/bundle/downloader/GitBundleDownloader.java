package org.entando.kubernetes.model.bundle.downloader;

import static java.util.Optional.ofNullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.SystemUtils;
import org.entando.kubernetes.model.debundle.EntandoDeBundleTag;

public class GitBundleDownloader extends BundleDownloader {

    private final String errorWhileFetchingTags = "An error occurred while fetching git repo tags";

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

    @Override
    protected Path saveBundleStrategy(URL url, Path targetPath) {
        try {
            String gitCommand = String.format("git clone --depth 1 %s %s",
                    url.toString(),
                    targetPath.toAbsolutePath());
            Process process = execGitCommands(gitCommand);

            //EDIT:
            // get Exit Status
            if (process.waitFor() != 0) {
                throw new BundleDownloaderException("An error occurred while shallow cloning the git repo");
            }
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

        String gitCommand = String.format("git clone --branch %s --depth 1 %s %s",
                tag.getVersion(),
                tag.getTarball(),
                targetPath.toAbsolutePath());
        Process process = execGitCommands(gitCommand);

        //EDIT:
        // get Exit Status
        if (process.waitFor() != 0) {
            throw new BundleDownloaderException("An error occurred while shallow cloning the git repo");
        }
    }

    @Override
    public List<String> fetchRemoteTags(URL repoUrl) {

        List<String> tagList = new ArrayList<>();

        String gitCommand = String.format("git ls-remote --tags %s", repoUrl.toString());

        Process process;
        try {
            process = execGitCommands(gitCommand, false);
            if (process.waitFor() != 0) {
                throw new BundleDownloaderException(errorWhileFetchingTags);
            }
        } catch (IOException e) {
            throw new BundleDownloaderException(errorWhileFetchingTags, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BundleDownloaderException(errorWhileFetchingTags, e);
        }

        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                final String[] tokens = line.split("refs/tags/");
                if (tokens.length > 0) {
                    tagList.add(tokens[1]);
                }
            }
        } catch (Exception e) {
            throw new BundleDownloaderException("An error occurred while reading fetched git repo tag list");
        }

        return tagList;
    }

    /**
     * receive a git shell command, compose the entire list of command to execute it in the shell and return the
     * corresponding list of string.
     *
     * @param gitCommand the shell git command to execute
     * @return the list of string containing the entire command to execute
     */
    private List<String> composeGitShellCommand(String gitCommand) {
        List<String> commands = new ArrayList<>();
        if (SystemUtils.IS_OS_WINDOWS) {
            commands.add("CMD");
            commands.add("/C");
        } else {
            commands.add("/bin/sh");
            commands.add("-c");
        }
        commands.add(gitCommand);
        return commands;
    }

    /**
     * compose and execute the received git command inheriting the java IO.
     * @param gitCommand the string identifying the git command to execute
     * @return the Process spawned by the command execution
     */
    private Process execGitCommands(String gitCommand) throws IOException {
        return execGitCommands(gitCommand, true);
    }

    /**
     * compose and execute the received git command.
     * @param gitCommand the string identifying the git command to execute
     * @param inheritIO if true the java IO is inherited by the command
     * @return the Process spawned by the command execution
     */
    private Process execGitCommands(String gitCommand, boolean inheritIO) throws IOException {
        List<String> commands = composeGitShellCommand(gitCommand);
        ProcessBuilder pb = new ProcessBuilder(commands);
        if (inheritIO) {
            pb.inheritIO();
        }
        //Propagate LD_PRELOAD to ensure SSH can work
        ofNullable(System.getenv("LD_PRELOAD")).ifPresent((s) -> pb.environment().put("LD_PRELOAD", s));
        return pb.start();
    }
}
