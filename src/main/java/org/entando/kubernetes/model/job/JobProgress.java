package org.entando.kubernetes.model.job;

public class JobProgress {

    double increment;
    int precision;
    double value;

    public JobProgress(double increment, int precision) {
        this.value = 0.0;
        this.increment = increment;
        this.precision = precision;
    }

    public JobProgress(double increment) {
        this.value = 0.0;
        this.increment = increment;
        this.precision = 2;
    }

    public void reset() {
        this.value = 0.0;
    }

    public void increment() {
        this.value = Math.min(1.0, getWithPrecision(this.value + increment));
    }

    public double getValue() {
        return Math.min(1.0, this.value);
    }

    private double getWithPrecision(double value) {
        int p = (int) Math.pow(10, this.precision);
        return Math.floor(value*p)/p;
    }
}
