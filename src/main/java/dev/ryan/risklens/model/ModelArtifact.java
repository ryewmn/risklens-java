package dev.ryan.risklens.model;

import java.util.List;
import java.util.Map;

public record ModelArtifact(
        String modelId,
        String version,
        String trainedAt,
        String trainingData,
        String intendedUse,
        double intercept,
        Calibration calibration,
        List<FeatureSpec> features,
        Map<String, Double> referenceNormalizedMeans) {

    public record Calibration(double slope, double intercept) {}

    public record FeatureSpec(
            String name,
            double mean,
            double scale,
            double coefficient,
            String reasonCode) {}
}
