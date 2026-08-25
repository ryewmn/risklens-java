package dev.ryan.risklens.api;

import static org.hamcrest.Matchers.hasKey;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class PredictionControllerTest {
    @Autowired MockMvc mvc;

    @Test
    void returnsTypedPredictionWithoutCaching() throws Exception {
        mvc.perform(post("/api/v1/predictions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"creditLimit":200000,"age":41,"repaymentStatusCurrent":0,
                             "repaymentStatusPrior":0,"billAmountCurrent":54000,
                             "billAmountPrior":51000,"paymentAmountCurrent":9000,
                             "paymentAmountPrior":8500}
                            """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(header().exists("X-Request-ID"))
                .andExpect(jsonPath("$.defaultProbability").isNumber())
                .andExpect(jsonPath("$.riskBand").value("MODERATE"))
                .andExpect(jsonPath("$.model.artifactSha256").isString());
    }

    @Test
    void returnsProblemDetailsForInvalidInput() throws Exception {
        mvc.perform(post("/api/v1/predictions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.violations", hasKey("creditLimit")));
    }
}
