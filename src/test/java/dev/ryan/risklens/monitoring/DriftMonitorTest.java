package dev.ryan.risklens.monitoring;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.ryan.risklens.api.ApplicantFeatures;
import dev.ryan.risklens.model.LogisticRiskModel;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class DriftMonitorTest {
    @Test
    void reportsOnlySafeAggregates() throws Exception {
        LogisticRiskModel model = new LogisticRiskModel(
                new ObjectMapper(), new ClassPathResource("models/logistic-v1.json"));
        DriftMonitor monitor = new DriftMonitor(model, 2, 0.18, 0.30);
        ApplicantFeatures input = new ApplicantFeatures(
                200_000.0, 41, 0, 0, 54_000.0, 51_000.0, 9_000.0, 8_500.0);

        monitor.observe(input);
        assertThat(monitor.report().status()).isEqualTo("INSUFFICIENT_DATA");
        monitor.observe(input);

        DriftMonitor.DriftReport report = monitor.report();
        assertThat(report.sampleCount()).isEqualTo(2);
        assertThat(report.featureShifts()).containsOnlyKeys(
                "repaymentStatusCurrent", "utilizationCurrent", "paymentRatioCurrent", "billTrend");
    }
}
