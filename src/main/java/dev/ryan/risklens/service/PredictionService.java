package dev.ryan.risklens.service;

import dev.ryan.risklens.api.ApplicantFeatures;
import dev.ryan.risklens.api.PredictionResponse;
import dev.ryan.risklens.api.RiskBand;
import dev.ryan.risklens.model.RiskModel;
import dev.ryan.risklens.monitoring.DriftMonitor;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public final class PredictionService {
    private static final Logger LOG = LoggerFactory.getLogger(PredictionService.class);
    private final RiskModel model;
    private final DriftMonitor drift;
    private final MeterRegistry registry;
    private final Timer inferenceTimer;

    public PredictionService(RiskModel model, DriftMonitor drift, MeterRegistry registry) {
        this.model = model;
        this.drift = drift;
        this.registry = registry;
        this.inferenceTimer = Timer.builder("risklens.inference.duration")
                .description("End-to-end model inference latency")
                .publishPercentileHistogram()
                .register(registry);
    }

    public PredictionResponse predict(ApplicantFeatures input) {
        String requestId = UUID.randomUUID().toString();
        long started = System.nanoTime();
        try {
            RiskModel.Prediction prediction = model.score(input);
            RiskBand band = RiskBand.fromProbability(prediction.probability());
            drift.observe(input);
            Counter.builder("risklens.predictions")
                    .tag("risk_band", band.name().toLowerCase())
                    .register(registry).increment();
            return new PredictionResponse(requestId, prediction.probability(), band,
                    prediction.reasonCodes(), model.metadata(), Instant.now());
        } finally {
            long elapsed = System.nanoTime() - started;
            inferenceTimer.record(elapsed, TimeUnit.NANOSECONDS);
            // Never log feature values, probabilities, or applicant identifiers.
            LOG.info("event=inference_completed requestId={} modelVersion={} latencyMicros={}",
                    requestId, model.metadata().version(), TimeUnit.NANOSECONDS.toMicros(elapsed));
        }
    }
}
