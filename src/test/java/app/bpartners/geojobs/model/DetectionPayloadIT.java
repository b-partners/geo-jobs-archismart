package app.bpartners.geojobs.model;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.bpartners.geojobs.conf.FacadeIT;
import app.bpartners.geojobs.service.detection.DetectionPayload;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class DetectionPayloadIT extends FacadeIT {
  @Autowired ObjectMapper om;

  private DetectionPayload detectionPayload() {
    return DetectionPayload.builder()
        .projectName("project")
        .fileName("filename")
        .base64ImgData("imageData")
        .build();
  }

  @Test
  void serialize_without_mask() throws JsonProcessingException {
    var serialized = om.writeValueAsString(detectionPayload());

    assertTrue(serialized.contains("\"projectname\":\"project\""));
    assertTrue(serialized.contains("\"filename\":\"filename\""));
    assertTrue(serialized.contains("\"base64_img_data\":\"imageData\""));
    assertFalse(serialized.contains("base64_mask_data"));
  }

  @Test
  void serialize_with_mask() throws JsonProcessingException {
    var payload = detectionPayload();
    payload.setBase64MaskData("mask");

    var serialized = om.writeValueAsString(payload);

    assertTrue(serialized.contains("\"base64_mask_data\":\"mask\""));
  }
}
