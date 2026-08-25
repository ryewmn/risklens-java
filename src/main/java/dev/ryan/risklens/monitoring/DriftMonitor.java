package dev.ryan.risklens.monitoring;

import dev.ryan.risklens.api.ApplicantFeatures;
import dev.ryan.risklens.model.RiskModel;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public final class DriftMonitor {
    private final RiskModel model;
    private final long minimumSamples;
    private final double warningThreshold;
    private final double criticalThreshold;
    private final Map<String, Double> sums = new LinkedHashMap<>();
    private long samples;

    public DriftMonitor(RiskModel model,
            @Value("${risklens.drift.minimum-samples:100}") long minimumSamples,
            @Value("${risklens.drift.warning-threshold:0.18}") double warningThreshold,
            @Value("${risklens.drift.critical-threshold:0.30}") double criticalThreshold) {
        this.model = model;
        this.minimumSamples = minimumSamples;
        this.warningThreshold = warningThreshold;
        this.criticalThreshold = criticalThreshold;
        model.referenceNormalizedMeans().keySet().forEach(name -> sums.put(name, 0.0));
    }

    /** Retains only running sums of normalized, non-identifying features. */
    public synchronized void observe(ApplicantFeatures input) {
        Map<String, Double> normalized = model.normalizedFeatures(input);
        sums.replaceAll((name, sum) -> sum + normalized.get(name));
        samples++;
    }

    public synchronized DriftReport report() {
        Map<String, Double> shifts = new LinkedHashMap<>();
        if (samples > 0) {
            sums.forEach((name, sum) -> shifts.put(name,
                    Math.abs((sum / samples) - model.referenceNormalizedMeans().getOrDefault(name, 0.0))));
        }
        double score = shifts.values().stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        String status = samples < minimumSamples ? "INSUFFICIENT_DATA"
                : score >= criticalThreshold ? "CRITICAL"
                : score >= warningThreshold ? "WARNING" : "STABLE";
        return new DriftReport(status, samples, minimumSamples, score, Map.copyOf(shifts), Instant.now());
    }

    public record DriftReport(
            String status,
            long sampleCount,
            long minimumSamples,
            double aggregateStandardizedMeanShift,
            Map<String, Double> featureShifts,
            Instant measuredAt) {}
}
