package app.bpartners.geojobs.snapstart;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import app.bpartners.geojobs.service.FilePolygonDrawer;
import app.bpartners.geojobs.service.TileImagesAssembler;
import app.bpartners.geojobs.service.detection.DetectionMaskCreator;
import app.bpartners.geojobs.service.geojson.GeometryConverter;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.client.RestTemplate;

class DetectionPathPrimerTest {
  private final DetectionMaskCreator maskCreator = spy(new DetectionMaskCreator());
  private final FilePolygonDrawer polygonDrawer = spy(new FilePolygonDrawer());
  private final TileImagesAssembler imagesAssembler = spy(new TileImagesAssembler());
  private final GeometryConverter geometryConverter = spy(new GeometryConverter());
  private final List<File> producedFiles = new CopyOnWriteArrayList<>();

  private final DetectionPathPrimer subject =
      new DetectionPathPrimer(
          maskCreator,
          polygonDrawer,
          imagesAssembler,
          geometryConverter,
          new ObjectMapper().findAndRegisterModules(),
          new RestTemplate());

  @Test
  void prime_runs_every_step_through_to_the_detection_api_call() {
    assertTrue(subject.prime(), "priming stopped early, see the warning it logged");

    verify(maskCreator, times(1)).apply(any());
    verify(polygonDrawer, times(1)).apply(any(), any(), any());
    verify(imagesAssembler, times(1)).apply(any());
    verify(geometryConverter, times(1)).getMultiPolygonFromTile(523561, 370292, 20);
    verify(geometryConverter, times(1)).readGeometryFromString(any());
  }

  @Test
  void prime_deletes_every_file_it_produced() {
    recordProducedFiles();

    assertTrue(subject.prime());

    assertFalse(producedFiles.isEmpty());
    producedFiles.forEach(
        file ->
            assertFalse(
                file.exists(), "primer left " + file.getAbsolutePath() + " behind in /tmp"));
  }

  @Test
  void prime_deletes_the_synthetic_tile_even_when_a_primed_step_fails() {
    var tileImageCaptor = ArgumentCaptor.forClass(File.class);
    doThrow(new RuntimeException("out of heap while drawing")).when(imagesAssembler).apply(any());

    assertFalse(subject.prime());

    verify(polygonDrawer).apply(any(), any(), tileImageCaptor.capture());
    var syntheticTileImage = tileImageCaptor.getValue();
    assertNotNull(syntheticTileImage);
    assertFalse(syntheticTileImage.exists());
  }

  @Test
  void before_checkpoint_primes_the_path_and_after_restore_is_harmless() {
    assertDoesNotThrow(() -> subject.beforeCheckpoint(null));
    verify(maskCreator, times(1)).apply(any());

    assertDoesNotThrow(() -> subject.afterRestore(null));
    verify(maskCreator, times(1)).apply(any());
  }

  @Test
  void prime_swallows_a_failure_instead_of_breaking_the_checkpoint() {
    doThrow(new RuntimeException("mask drawing unavailable")).when(maskCreator).apply(any());

    assertDoesNotThrow(() -> subject.beforeCheckpoint(null));
    assertFalse(subject.prime());
  }

  private void recordProducedFiles() {
    doAnswer(recordingAnswer()).when(maskCreator).apply(any());
    doAnswer(recordingAnswer()).when(polygonDrawer).apply(any(), any(), any());
    doAnswer(recordingAnswer()).when(imagesAssembler).apply(any());
  }

  private org.mockito.stubbing.Answer<File> recordingAnswer() {
    return invocation -> {
      var produced = (File) invocation.callRealMethod();
      producedFiles.add(produced);
      return produced;
    };
  }
}
