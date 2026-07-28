package app.bpartners.geojobs.service;

import static app.bpartners.geojobs.endpoint.rest.model.DelimitationType.USER_DEFINED_DELIMITATION;
import static app.bpartners.geojobs.repository.model.cityjson.CityJSONDelimitationObjectType.BUILDING_ROOF;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import app.bpartners.geojobs.file.ExtensionGuesser;
import app.bpartners.geojobs.file.FileWriter;
import app.bpartners.geojobs.file.bucket.BucketComponent;
import app.bpartners.geojobs.repository.CommunityAuthorizationRepository;
import app.bpartners.geojobs.repository.model.cityjson.CityJSONRequest;
import app.bpartners.geojobs.repository.model.community.CommunityAuthorization;
import app.bpartners.geojobs.repository.model.community.CommunityAuthorizationApiKey;
import app.bpartners.geojobs.service.cityjson.texture.CityJsonTextureComputer;
import app.bpartners.geojobs.service.ciytjsonprocessor.CityJsonProcessorApiClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CityJSONInternalProcessorTest {
  private final BucketComponent bucketComponentMock = mock();
  private final CityJsonProcessorApiClient apiClientMock = mock();
  private final CityJsonTextureComputer textureComputerMock = mock();
  private final CityJSONInternalProcessor.CityJSONDownloader cityJSONDownloaderMock = mock();
  private final CommunityAuthorizationRepository caRepositoryMock = mock();

  private final CityJSONInternalProcessor subject =
      new CityJSONInternalProcessor(
          new FileWriter(new ObjectMapper(), new ExtensionGuesser()),
          bucketComponentMock,
          textureComputerMock,
          apiClientMock,
          cityJSONDownloaderMock,
          caRepositoryMock);

  @BeforeEach
  void setup() {
    when(bucketComponentMock.upload(any(), any())).thenReturn(mock());
    when(bucketComponentMock.presign(any(), any())).thenReturn(mock());
    when(apiClientMock.generate(any(), any())).thenReturn(mock());
    when(textureComputerMock.applyTexture(any(), any())).then(mock());
    when(cityJSONDownloaderMock.download(any())).thenReturn(mock());

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
}
