package org.entando.kubernetes.model.bundle.downloader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ProcessHandler {

    private List<ProcessBuilder> pipeline = null;
    private ProcessBuilder command = null;
    private Process commandProcess = null;

    public ProcessHandler(ProcessBuilder pb) {
        command = pb;
    }

    public ProcessHandler(List<ProcessBuilder> pbs) {
        pipeline = pbs;
    }

    /**
     * This method executes the configured process.
     *
     * @return itself
     * @throws IOException if an I/O error occurs
     */
    public ProcessHandler start() throws IOException {
        if (command != null) {
            commandProcess = command.start();
        } else if (pipeline != null) {
            List<Process> pipelineProcesses = ProcessBuilder.startPipeline(pipeline);
            commandProcess = pipelineProcesses.get(pipelineProcesses.size() - 1);
        }
        return this;
    }

    /**
     * This method waits for the configured process to finish.
     *
     * @param timeoutSeconds the maximum waiting time in seconds
     * @return itself
     * @throws InterruptedException if the current thread is interrupted while waiting.
     */
    public ProcessHandler waitFor(int timeoutSeconds)
            throws InterruptedException {

        commandProcess.waitFor(timeoutSeconds, TimeUnit.SECONDS);
        if (commandProcess.isAlive()) {
            commandProcess.destroy();
            commandProcess.waitFor(100, TimeUnit.MILLISECONDS);
            if (commandProcess.isAlive()) {
                commandProcess.destroyForcibly();
            }
        }
        return this;
    }

    /**
     * Returns the exit value for the process.
     *
     * @return the exit value of the process
     */
    public int exitValue() {
        return commandProcess.exitValue();
    }

    /**
     * Returns the output values for the process.
     *
     * @return a list of lines representing the output values of the process
     * @throws IOException if an I/O error occurs
     */
    public List<String> getOutputLines() throws IOException {
        List<String> lines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(commandProcess.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line.trim());
            }
        }
        return lines;
    }
}
