package org.entando.kubernetes.model.bundle.downloader;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.SystemUtils;

@Slf4j
public final class ProcessHandlerBuilder {

    private ProcessHandlerBuilder() {
    }

    /**
     * Return the ProcessHandler for the pipelined commands. It doesn't inherit I/O to the current thread.
     *
     * @param pipelineSteps the list of commands to execute in pipeline
     * @return the ProcessHandler to use to handle process execution and output
     */
    public static ProcessHandler buildPipelineShellCommand(List<String> pipelineSteps) {
        return buildPipelineShellCommand(pipelineSteps, false);
    }

    /**
     * Return the ProcessHandler for the pipelined commands.
     *
     * @param pipelineSteps the list of commands to execute in pipeline
     * @param inheritIO     the boolean to enable the inheritance of I/O to the current process
     * @return the ProcessHandler to use to handle process execution and output
     */
    public static ProcessHandler buildPipelineShellCommand(List<String> pipelineSteps, boolean inheritIO) {
        List<ProcessBuilder> pbs = new ArrayList<>();
        for (int i = 0; i < pipelineSteps.size(); i++) {
            ProcessBuilder pb = composeShellCommand(pipelineSteps.get(i), inheritIO);
            if (i == 0) {
                // first
                pb.redirectOutput(ProcessBuilder.Redirect.PIPE);
            } else if (i == pipelineSteps.size() - 1) {
                // last
                pb.redirectInput(ProcessBuilder.Redirect.PIPE);
                pb.redirectError(ProcessBuilder.Redirect.INHERIT);
            } else {
                pb.redirectInput(ProcessBuilder.Redirect.PIPE).redirectOutput(ProcessBuilder.Redirect.PIPE);
            }
            pbs.add(pb);
        }
        return new ProcessHandler(pbs);
    }

    /**
     * Return the ProcessHandler for the shell command. The inheritance of I/O to the current process is active by
     * default.
     *
     * @param command    the command to execute in a shell environment
     * @param paramsList the list of the command parameters
     * @return the ProcessHandler to use to handle process execution and output
     */
    public static ProcessHandler buildShellCommand(String command, List<String> paramsList) {
        return buildShellCommand(command, paramsList, true);
    }

    /**
     * Return the ProcessHandler for the shell command.
     *
     * @param command    the command to execute in a shell environment
     * @param paramsList the list of the command parameters
     * @param inheritIO  the boolean to enable the inheritance of I/O to the current process
     * @return the ProcessHandler to use to handle process execution and output
     */
    public static ProcessHandler buildShellCommand(String command, List<String> paramsList, boolean inheritIO) {
        String joinCommand =
                paramsList.isEmpty() ? command : command + " " + paramsList.stream().collect(Collectors.joining(" "));
        return new ProcessHandler(composeShellCommand(joinCommand, inheritIO));
    }

    /**
     * Return the ProcessHandler for the command. The inheritance of I/O to the current process is active by default.
     *
     * @param command    the command to execute
     * @param paramsList the list of the command parameters
     * @return the ProcessHandler to use to handle process execution and output
     */
    public static ProcessHandler buildCommand(String command, List<String> paramsList) {
        return buildCommand(command, paramsList, true);
    }

    /**
     * Return the ProcessHandler for the command.
     *
     * @param command    the command to execute
     * @param paramsList the list of the command parameters
     * @param inheritIO  the boolean to enable the inheritance of I/O to the current process
     * @return the ProcessHandler to use to handle process execution and output
     */
    public static ProcessHandler buildCommand(String command, List<String> paramsList, boolean inheritIO) {
        return new ProcessHandler(execCommand(command, paramsList, false, inheritIO));
    }

    private static ProcessBuilder execCommand(String command, List<String> paramsList, boolean isShellCommand,
            boolean inheritIO) {
        ProcessBuilder pb = isShellCommand ? composeShellCommand(command, true) : new ProcessBuilder(command);
        paramsList.forEach(p -> pb.command().add(p));

        if (inheritIO) {
            pb.inheritIO();
        }
        return pb;
    }

    private static ProcessBuilder composeShellCommand(String command, boolean inheritIO) {
        ProcessBuilder pb = null;
        if (SystemUtils.IS_OS_WINDOWS) {
            pb = new ProcessBuilder("CMD");
            pb.command().add("/C");

        } else {
            pb = new ProcessBuilder("/bin/sh");
            pb.command().add("-c");
        }
        pb.command().add(command);
        if (inheritIO) {
            pb.inheritIO();
        }
        return pb;
    }

}
