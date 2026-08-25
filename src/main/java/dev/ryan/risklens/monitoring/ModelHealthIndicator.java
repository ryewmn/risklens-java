package dev.ryan.risklens.monitoring;

import dev.ryan.risklens.model.RiskModel;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component("riskModel")
public final class ModelHealthIndicator implements HealthIndicator {
    private final RiskModel model;

    public ModelHealthIndicator(RiskModel model) {
        this.model = model;
    }

    @Override
    public Health health() {
        return Health.up()
                .withDetail("modelId", model.metadata().id())
                .withDetail("modelVersion", model.metadata().version())
                .withDetail("engine", model.metadata().engine())
                .build();
    }
}
