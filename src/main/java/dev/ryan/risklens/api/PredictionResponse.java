package dev.ryan.risklens.api;

import dev.ryan.risklens.model.ModelMetadata;
import java.time.Instant;
import java.util.List;

public record PredictionResponse(
        String requestId,
        double defaultProbability,
        RiskBand riskBand,
        List<String> reasonCodes,
        ModelMetadata model,
        Instant predictedAt) {
}
