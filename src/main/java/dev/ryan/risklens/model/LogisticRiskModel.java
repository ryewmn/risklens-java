package dev.ryan.risklens.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.ryan.risklens.api.ApplicantFeatures;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

@Component
@Profile("!onnx")
public final class LogisticRiskModel implements RiskModel {
    private final ModelArtifact artifact;
    private final ModelMetadata metadata;

    public LogisticRiskModel(ObjectMapper mapper,
            @Value("${risklens.model-resource}") Resource resource) throws IOException {
        byte[] bytes;
        try (InputStream input = resource.getInputStream()) {
            bytes = input.readAllBytes();
        }
        this.artifact = mapper.readValue(bytes, ModelArtifact.class);
        validateArtifact(artifact);
        this.metadata = new ModelMetadata(
                artifact.modelId(), artifact.version(), sha256(bytes), artifact.trainedAt(), "logistic-json");
    }

    @Override
    public Prediction score(ApplicantFeatures input) {
        Map<String, Double> normalized = normalizedFeatures(input);
        double rawLogit = artifact.intercept();
        Map<ModelArtifact.FeatureSpec, Double> contributions = new LinkedHashMap<>();
        for (ModelArtifact.FeatureSpec feature : artifact.features()) {
            double contribution = feature.coefficient() * normalized.get(feature.name());
            rawLogit += contribution;
            contributions.put(feature, contribution);
        }
        double calibratedLogit = artifact.calibration().intercept() + artifact.calibration().slope() * rawLogit;
        double probability = sigmoid(calibratedLogit);
        List<String> reasons = contributions.entrySet().stream()
                .filter(entry -> entry.getValue() > 0.0)
                .sorted(Map.Entry.<ModelArtifact.FeatureSpec, Double>comparingByValue(Comparator.reverseOrder()))
                .limit(3)
                .map(entry -> entry.getKey().reasonCode())
                .toList();
        return new Prediction(probability, reasons);
    }

    @Override
    public Map<String, Double> normalizedFeatures(ApplicantFeatures input) {
        Map<String, Double> raw = Map.of(
                "age", input.age().doubleValue(),
                "logCreditLimit", Math.log1p(input.creditLimit()),
                "repaymentStatusCurrent", input.repaymentStatusCurrent().doubleValue(),
                "repaymentStatusPrior", input.repaymentStatusPrior().doubleValue(),
                "utilizationCurrent", Math.max(0, input.billAmountCurrent()) / input.creditLimit(),
                "utilizationPrior", Math.max(0, input.billAmountPrior()) / input.creditLimit(),
                "paymentRatioCurrent", input.paymentAmountCurrent() / Math.max(1, Math.abs(input.billAmountCurrent())),
                "paymentRatioPrior", input.paymentAmountPrior() / Math.max(1, Math.abs(input.billAmountPrior())),
                "billTrend", (input.billAmountCurrent() - input.billAmountPrior()) / input.creditLimit());
        Map<String, Double> normalized = new LinkedHashMap<>();
        for (ModelArtifact.FeatureSpec feature : artifact.features()) {
            Double value = raw.get(feature.name());
            if (value == null) throw new IllegalStateException("Unknown model feature: " + feature.name());
            normalized.put(feature.name(), (value - feature.mean()) / feature.scale());
        }
        return Map.copyOf(normalized);
    }

    @Override public ModelMetadata metadata() { return metadata; }
    @Override public Map<String, Double> referenceNormalizedMeans() { return artifact.referenceNormalizedMeans(); }

    private static double sigmoid(double value) {
        if (value >= 0) return 1.0 / (1.0 + Math.exp(-value));
        double exponential = Math.exp(value);
        return exponential / (1.0 + exponential);
    }

    private static void validateArtifact(ModelArtifact artifact) {
        if (artifact.features() == null || artifact.features().isEmpty()) {
            throw new IllegalArgumentException("Model artifact must contain features");
        }
        if (artifact.calibration() == null || artifact.calibration().slope() <= 0) {
            throw new IllegalArgumentException("Model calibration is invalid");
        }
        for (ModelArtifact.FeatureSpec feature : artifact.features()) {
            if (feature.scale() <= 0 || !Double.isFinite(feature.coefficient())) {
                throw new IllegalArgumentException("Invalid model feature: " + feature.name());
            }
        }
    }

    private static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is unavailable", impossible);
        }
    }
}
