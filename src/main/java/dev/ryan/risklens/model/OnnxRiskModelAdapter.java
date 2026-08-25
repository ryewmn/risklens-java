package dev.ryan.risklens.model;

import dev.ryan.risklens.api.ApplicantFeatures;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Framework-neutral adapter used by the optional ONNX deployment profile. */
public final class OnnxRiskModelAdapter implements RiskModel {
    private static final List<String> ORDER = List.of(
            "age", "logCreditLimit", "repaymentStatusCurrent", "repaymentStatusPrior",
            "utilizationCurrent", "utilizationPrior", "paymentRatioCurrent",
            "paymentRatioPrior", "billTrend");
    private final OnnxScoringPort port;
    private final ModelArtifact artifact;

    public OnnxRiskModelAdapter(OnnxScoringPort port, ModelArtifact artifact) {
        this.port = port;
        this.artifact = artifact;
    }

    @Override
    public Prediction score(ApplicantFeatures input) {
        Map<String, Double> normalized = normalizedFeatures(input);
        float[] vector = new float[ORDER.size()];
        for (int index = 0; index < ORDER.size(); index++) {
            vector[index] = normalized.get(ORDER.get(index)).floatValue();
        }
        OnnxScoringPort.Result result = port.score(vector);
        if (!Double.isFinite(result.calibratedProbability())
                || result.calibratedProbability() < 0 || result.calibratedProbability() > 1) {
            throw new IllegalStateException("ONNX provider returned an invalid probability");
        }
        return new Prediction(result.calibratedProbability(), List.copyOf(result.reasonCodes()));
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
        Map<String, Double> result = new LinkedHashMap<>();
        for (ModelArtifact.FeatureSpec spec : artifact.features()) {
            result.put(spec.name(), (raw.get(spec.name()) - spec.mean()) / spec.scale());
        }
        return Map.copyOf(result);
    }

    @Override
    public ModelMetadata metadata() {
        return new ModelMetadata(artifact.modelId(), artifact.version(), port.modelSha256(),
                artifact.trainedAt(), "onnx-runtime-" + port.runtimeVersion());
    }

    @Override
    public Map<String, Double> referenceNormalizedMeans() {
        return artifact.referenceNormalizedMeans();
    }
}
