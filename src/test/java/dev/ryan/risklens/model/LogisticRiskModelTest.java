package dev.ryan.risklens.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.ryan.risklens.api.ApplicantFeatures;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class LogisticRiskModelTest {
    private final ObjectMapper mapper = new ObjectMapper();
    private LogisticRiskModel model;

    @BeforeEach
    void setUp() throws Exception {
        model = new LogisticRiskModel(mapper, new ClassPathResource("models/logistic-v1.json"));
    }

    @Test
    void matchesPythonReferenceCases() throws Exception {
        try (InputStream stream = new ClassPathResource("parity-cases.json").getInputStream()) {
            List<Map<String, Object>> cases = mapper.readValue(stream, new TypeReference<>() {});
            for (Map<String, Object> testCase : cases) {
                ApplicantFeatures input = mapper.convertValue(testCase.get("input"), ApplicantFeatures.class);
                double expected = ((Number) testCase.get("expectedProbability")).doubleValue();
                assertThat(model.score(input).probability())
                        .as((String) testCase.get("name"))
                        .isCloseTo(expected, within(1e-12));
            }
        }
    }

    @Test
    void producesStableMetadataAndRankedReasons() {
        ApplicantFeatures highRisk = new ApplicantFeatures(
                50_000.0, 27, 3, 2, 49_000.0, 46_000.0, 500.0, 700.0);
        RiskModel.Prediction prediction = model.score(highRisk);

        assertThat(prediction.probability()).isBetween(0.97, 0.98);
        assertThat(prediction.reasonCodes()).contains("CURRENT_PAYMENT_DELAY");
        assertThat(model.metadata().artifactSha256()).hasSize(64);
        assertThat(model.metadata().engine()).isEqualTo("logistic-json");
    }
}
