package app.bpartners.geojobs.service.detection;

import static app.bpartners.geojobs.repository.model.detection.DetectableType.PLACE_STANDARD;
import static app.bpartners.geojobs.repository.model.detection.DetectableType.VELUX;
import static app.bpartners.geojobs.service.detection.DetectionApiVersion.V2;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.bpartners.geojobs.file.bucket.BucketConf;
import app.bpartners.geojobs.file.bucket.CustomBucketComponent;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.util.List;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;

class LambdaFallbackConfTest {
  private static final String BUCKET_NAME = "dummyBucket";

  CustomBucketComponent bucketComponentMock = mock(CustomBucketComponent.class);
  BucketConf bucketConfMock = mock(BucketConf.class);
  LambdaFallbackConf subject = new LambdaFallbackConf(bucketComponentMock, new ObjectMapper());

  @Test
  @SneakyThrows
  void loads_fallback_urls_once_and_deletes_downloaded_file() {
    var file = Files.createTempFile("lambdaFallback", ".json");
    Files.writeString(
        file,
        """
        [
          {"objectType": "VELUX", "url": "https://velux", "version": "V2"},
          {"objectType": "PLACE_STANDARD", "url": "https://place", "version": "V2"}
        ]""");
    when(bucketComponentMock.getBucketConf()).thenReturn(bucketConfMock);
    when(bucketConfMock.getBucketName()).thenReturn(BUCKET_NAME);
    when(bucketComponentMock.download(any(String.class), any(String.class)))
        .thenReturn(file.toFile());

    var first = subject.getTileDetectorUrls();
    var second = subject.getTileDetectorUrls();

    assertEquals(
        List.of(
            TileDetectorUrl.builder().objectType(VELUX).url("https://velux").version(V2).build(),
            TileDetectorUrl.builder()
                .objectType(PLACE_STANDARD)
                .url("https://place")
                .version(V2)
                .build()),
        first);
    assertEquals(first, second);
    assertFalse(file.toFile().exists());
    verify(bucketComponentMock, times(1)).download(any(String.class), any(String.class));
  }
}
