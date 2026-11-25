package app.bpartners.geojobs.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import app.bpartners.geojobs.service.dashboard.component.AnalysisProject;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class AnalysisProjectComponentTest {
  static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  @Test
  void response_without_credit_balance_has_no_credits_information() throws Exception {
    var actual =
        OBJECT_MAPPER.readValue(
            "{\"type\":\"500 INTERNAL_SERVER_ERROR\",\"message\":\"Unexpected error\"}",
            AnalysisProject.class);

    assertNull(actual.creditsSufficient());
    assertNull(actual.creditCost());
  }

  @Test
  void response_with_credit_balance_is_read() throws Exception {
    var actual =
        OBJECT_MAPPER.readValue(
            "{\"projectKey\":\"key\",\"creditCost\":1,\"creditsSufficient\":false}",
            AnalysisProject.class);

    assertEquals("key", actual.projectKey());
    assertEquals(1L, actual.creditCost());
    assertEquals(false, actual.creditsSufficient());
  }
}
