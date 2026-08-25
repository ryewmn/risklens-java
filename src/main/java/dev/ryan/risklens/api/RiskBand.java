package dev.ryan.risklens.api;

public enum RiskBand {
    LOW, MODERATE, HIGH, CRITICAL;

    public static RiskBand fromProbability(double probability) {
        if (probability < 0.20) return LOW;
        if (probability < 0.50) return MODERATE;
        if (probability < 0.75) return HIGH;
        return CRITICAL;
    }
}
