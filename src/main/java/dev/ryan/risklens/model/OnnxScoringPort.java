package dev.ryan.risklens.model;

import java.util.List;

/**
 * Stable boundary for an ONNX Runtime provider. The core service deliberately
 * does not depend on native ONNX binaries; a deployment-specific adapter can
 * implement this port under the {@code onnx} Spring profile.
 */
public interface OnnxScoringPort {
    Result score(float[] orderedNormalizedFeatures);
    String runtimeVersion();
    String modelSha256();

    record Result(double calibratedProbability, List<String> reasonCodes) {}
}
