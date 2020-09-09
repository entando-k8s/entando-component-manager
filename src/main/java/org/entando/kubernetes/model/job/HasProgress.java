package org.entando.kubernetes.model.job;

public interface HasProgress {

    double getProgress();

    void setProgress(double newProgress);
}
