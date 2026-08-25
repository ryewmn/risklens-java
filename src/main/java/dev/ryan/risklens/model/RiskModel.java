package dev.ryan.risklens.model;

import dev.ryan.risklens.api.ApplicantFeatures;
import java.util.List;
import java.util.Map;

public interface RiskModel {
    Prediction score(ApplicantFeatures input);
    ModelMetadata metadata();
    Map<String, Double> normalizedFeatures(ApplicantFeatures input);
    Map<String, Double> referenceNormalizedMeans();

    record Prediction(double probability, List<String> reasonCodes) {}
}
