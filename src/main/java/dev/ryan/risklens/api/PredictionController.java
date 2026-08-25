package dev.ryan.risklens.api;

import dev.ryan.risklens.model.ModelMetadata;
import dev.ryan.risklens.model.RiskModel;
import dev.ryan.risklens.monitoring.DriftMonitor;
import dev.ryan.risklens.service.PredictionService;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public final class PredictionController {
    private final PredictionService service;
    private final RiskModel model;
    private final DriftMonitor drift;

    public PredictionController(PredictionService service, RiskModel model, DriftMonitor drift) {
        this.service = service;
        this.model = model;
        this.drift = drift;
    }

    @PostMapping("/predictions")
    public ResponseEntity<PredictionResponse> predict(@Valid @RequestBody ApplicantFeatures input) {
        PredictionResponse response = service.predict(input);
        return ResponseEntity.created(URI.create("/api/v1/predictions/" + response.requestId()))
                .cacheControl(CacheControl.noStore())
                .header("X-Request-ID", response.requestId())
                .body(response);
    }

    @GetMapping("/model")
    public ModelMetadata model() {
        return model.metadata();
    }

    @GetMapping("/model/drift")
    public DriftMonitor.DriftReport drift() {
        return drift.report();
    }
}
