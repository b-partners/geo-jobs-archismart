package app.bpartners.geojobs.service;

import static app.bpartners.geojobs.endpoint.rest.model.DelimitationType.USER_DEFINED_DELIMITATION;
import static app.bpartners.geojobs.repository.model.cityjson.CityJSONDelimitationObjectType.BUILDING_ROOF;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import app.bpartners.geojobs.file.ExtensionGuesser;
import app.bpartners.geojobs.file.FileWriter;
import app.bpartners.geojobs.file.bucket.BucketComponent;
import app.bpartners.geojobs.file.hash.FileHash;
import app.bpartners.geojobs.repository.CommunityAuthorizationRepository;
import app.bpartners.geojobs.repository.model.cityjson.CityJSONRequest;
import app.bpartners.geojobs.repository.model.community.CommunityAuthorization;
import app.bpartners.geojobs.repository.model.community.CommunityAuthorizationApiKey;
import app.bpartners.geojobs.service.cityjson.texture.CityJsonTextureComputer;
import app.bpartners.geojobs.service.threed.ThreedApiClient;
import app.bpartners.geojobs.service.threed.exception.ThreedApiException;
import app.bpartners.geojobs.service.threed.model.GenerationStatus;
import app.bpartners.geojobs.service.threed.model.Lrg;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;

class CityJSONThreedProcessorTest {
  private final BucketComponent bucketComponentMock = mock();
  private final ThreedApiClient apiClientMock = mock();
  private final CityJsonTextureComputer textureComputerMock = mock();
  private final CityJSONThreedProcessor.CityJSONDownloader cityJSONDownloaderMock = mock();
  private final CommunityAuthorizationRepository caRepositoryMock = mock();

  private final CityJSONThreedProcessor subject =
      new CityJSONThreedProcessor(
          new FileWriter(new ObjectMapper(), new ExtensionGuesser()),
          bucketComponentMock,
          textureComputerMock,
          apiClientMock,
          cityJSONDownloaderMock,
          caRepositoryMock);

  @BeforeEach
  void setup() {
    FileHash uploadedFileHash = mock();
    URL presignedUrl = mock();
    Lrg generatedLrg = mock();
    Answer<File> textureAnswer = mock();
    File downloadedFile = mock();

    when(bucketComponentMock.upload(any(), any())).thenReturn(uploadedFileHash);
    when(bucketComponentMock.presign(any(), any())).thenReturn(presignedUrl);
    when(apiClientMock.generate(any(), any())).thenReturn(generatedLrg);
    when(textureComputerMock.applyTexture(any(), any())).then(textureAnswer);
    when(cityJSONDownloaderMock.download(any())).thenReturn(downloadedFile);

    var communityApiKey =
        CommunityAuthorizationApiKey.builder().keyValue(randomUUID().toString()).build();
    var community = CommunityAuthorization.builder().apiKeys(List.of(communityApiKey)).build();
    when(caRepositoryMock.findById(any())).thenReturn(Optional.of(community));
  }

  @Test
  void should_call_internal_processor_when_roofer_throws() {
    var actual =
        subject.apply(
            CityJSONRequest.builder()
                .id(randomUUID().toString())
                .delimitationType(USER_DEFINED_DELIMITATION)
                .delimitationObjectType(BUILDING_ROOF)
                .delimitations(List.of())
                .build());

    assertNotNull(actual);
  }

  @Test
  void degraded_generation_status_does_not_throw() {
    var degradedLrg =
        Lrg.builder()
            .status(GenerationStatus.DEGRADED)
            .message("1 roof panel(s) had no lidar coverage")
            .build();

    assertDoesNotThrow(() -> CityJSONThreedProcessor.checkGenerationStatus(degradedLrg));
  }

  @Test
  void no_lidar_generation_status_throws_as_no_lidar_data_found() {
    var noLidarLrg =
        Lrg.builder()
            .status(GenerationStatus.NO_LIDAR)
            .message("No lidar point covers the requested footprint")
            .build();

    var exception =
        assertThrows(
            ThreedApiException.class,
            () -> CityJSONThreedProcessor.checkGenerationStatus(noLidarLrg));
    assertTrue(exception.isNoLidarDataFound());
  }

  @Test
  void failed_generation_status_throws() {
    var failedLrg = Lrg.builder().status(GenerationStatus.FAILED).message("boom").build();

    assertThrows(
        ThreedApiException.class, () -> CityJSONThreedProcessor.checkGenerationStatus(failedLrg));
  }
}
