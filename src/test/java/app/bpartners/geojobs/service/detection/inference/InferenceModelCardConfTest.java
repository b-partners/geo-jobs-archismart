package app.bpartners.geojobs.service.detection.inference;

import static app.bpartners.geojobs.repository.model.detection.DetectableType.HUMIDITE;
import static app.bpartners.geojobs.repository.model.detection.DetectableType.TOMBE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.bpartners.geojobs.file.bucket.BucketComponent;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.nio.file.Files;
import java.util.List;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.awscore.exception.AwsServiceException;

class InferenceModelCardConfTest {
  BucketComponent bucketComponent = mock();
  InferenceModelCardConf subject = new InferenceModelCardConf(bucketComponent, new ObjectMapper());

  @Test
  @SneakyThrows
  void loads_mappings_once_and_deletes_downloaded_file() {
    var file =
        confFile(
            """
            [
              {"objectType": "HUMIDITE", "modelCardId": "damages-ignore-index-c4",
               "zoomLevel": 21, "spatialResolution": 5},
              {"objectType": "TOMBE", "modelCardId": "cimetiere-tombe-v1.0",
               "zoomLevel": 20, "spatialResolution": 7.5, "unknown": 1}
            ]""");
    when(bucketComponent.download("conf/" + System.getenv("ENV") + "/inferenceModelCards.json"))
        .thenReturn(file);

    var first = subject.getMappings();
    var second = subject.getMappings();

    assertEquals(
        List.of(
            new ModelCardMapping(HUMIDITE, "damages-ignore-index-c4", 21, 5.0),
            new ModelCardMapping(TOMBE, "cimetiere-tombe-v1.0", 20, 7.5)),
        first);
    assertEquals(first, second);
    assertFalse(file.exists());
    verify(bucketComponent, times(1)).download(any(String.class));
  }

  @Test
  void download_failed_and_throws_exception() {
    when(bucketComponent.download(any(String.class))).thenThrow(AwsServiceException.class);

    assertThrows(AwsServiceException.class, () -> subject.getMappings());
  }

  @Test
  void rejects_mapping_without_zoom_level() {
    givenConf(
        """
        [{"objectType": "HUMIDITE", "modelCardId": "damages", "spatialResolution": 5}]""");

    assertThrows(IllegalStateException.class, () -> subject.getMappings());
  }

  @Test
  void rejects_mapping_without_spatial_resolution() {
    givenConf(
        """
        [{"objectType": "HUMIDITE", "modelCardId": "damages", "zoomLevel": 21}]""");

    assertThrows(IllegalStateException.class, () -> subject.getMappings());
  }

  @Test
  void rejects_same_model_card_mapped_with_different_zoom_levels() {
    givenConf(
        """
[
  {"objectType": "HUMIDITE", "modelCardId": "damages", "zoomLevel": 21, "spatialResolution": 5},
  {"objectType": "USURE", "modelCardId": "damages", "zoomLevel": 20, "spatialResolution": 5}
]""");

    assertThrows(IllegalStateException.class, () -> subject.getMappings());
  }

  @Test
  void accepts_same_model_card_mapped_consistently_for_several_types() {
    givenConf(
        """
[
  {"objectType": "HUMIDITE", "modelCardId": "damages", "zoomLevel": 21, "spatialResolution": 5},
  {"objectType": "USURE", "modelCardId": "damages", "zoomLevel": 21, "spatialResolution": 5}
]""");

    assertEquals(2, subject.getMappings().size());
  }

  @Test
  void accepts_same_type_mapped_for_several_spatial_resolutions() {
    givenConf(
        """
[
  {"objectType": "OBSTACLE", "modelCardId": "obstacles5", "zoomLevel": 20, "spatialResolution": 5},
  {"objectType": "OBSTACLE", "modelCardId": "obstacles10", "zoomLevel": 19, "spatialResolution": 10},
  {"objectType": "OBSTACLE", "modelCardId": "obstacles20", "zoomLevel": 18, "spatialResolution": 20}
]""");

    assertEquals(3, subject.getMappings().size());
  }

  @Test
  void rejects_same_type_mapped_twice_for_the_same_spatial_resolution() {
    givenConf(
        """
[
  {"objectType": "OBSTACLE", "modelCardId": "obstacles", "zoomLevel": 20, "spatialResolution": 5},
  {"objectType": "OBSTACLE", "modelCardId": "obstacles-v2", "zoomLevel": 20, "spatialResolution": 5}
]""");

    assertThrows(IllegalStateException.class, () -> subject.getMappings());
  }

  private void givenConf(String content) {
    when(bucketComponent.download(any(String.class))).thenReturn(confFile(content));
  }

  @SneakyThrows
  private File confFile(String content) {
    var file = Files.createTempFile("inferenceModelCards", ".json");
    Files.writeString(file, content);
    return file.toFile();
  }
}
