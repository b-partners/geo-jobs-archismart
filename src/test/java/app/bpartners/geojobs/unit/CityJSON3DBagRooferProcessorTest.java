package app.bpartners.geojobs.unit;

import static app.bpartners.geojobs.endpoint.rest.controller.v1.mapper.FeatureMapper.toDomainFeature;
import static app.bpartners.geojobs.endpoint.rest.model.Feature.TypeEnum.FEATURE;
import static app.bpartners.geojobs.service.cityjson.model.object.CityJsonIO.computeAdditionalProperties;
import static app.bpartners.geojobs.service.cityjson.model.object.CityJsonIO.write;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import app.bpartners.geojobs.endpoint.rest.controller.v1.mapper.FeatureMapper;
import app.bpartners.geojobs.endpoint.rest.model.Feature;
import app.bpartners.geojobs.endpoint.rest.model.FeatureGeometry;
import app.bpartners.geojobs.endpoint.rest.model.MultiPolygon;
import app.bpartners.geojobs.file.FileWriter;
import app.bpartners.geojobs.file.bucket.BucketComponent;
import app.bpartners.geojobs.repository.model.cityjson.CityJSON;
import app.bpartners.geojobs.repository.model.cityjson.CityJSONRequest;
import app.bpartners.geojobs.service.CityJSON3DBagRooferProcessor;
import app.bpartners.geojobs.service.CoordinateTransformer;
import app.bpartners.geojobs.service.cityjson.model.object.CityJsonIO;
import app.bpartners.geojobs.service.cityjson.texture.CityJsonTextureComputer;
import app.bpartners.geojobs.service.geojson.GeometryConverter;
import app.bpartners.geojobs.service.lidar.api.LidarApiFacade;
import app.bpartners.geojobs.service.lidar.api.SwissBoundaryChecker;
import app.bpartners.geojobs.service.roofer3dbag.Roofer3DBagApiClient;
import app.bpartners.geojobs.service.roofer3dbag.model.CityJsonGenerationRequest;
import app.bpartners.geojobs.service.roofer3dbag.model.CityJsonGenerationResponse;
import app.bpartners.geojobs.service.roofer3dbag.validator.Roofer3DBagCityJSONValidator;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Geometry;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.core.io.ClassPathResource;

class CityJSON3DBagRooferProcessorTest {
  BucketComponent bucketComponentMock = mock(BucketComponent.class);
  FeatureMapper featureMapperMock = mock(FeatureMapper.class);
  LidarApiFacade lidarApiFacadeMock = mock(LidarApiFacade.class);
  Roofer3DBagApiClient roofer3DBagApiClientMock = mock(Roofer3DBagApiClient.class);
  FileWriter fileWriterMock = mock(FileWriter.class);
  CoordinateTransformer coordinateTransformer = new CoordinateTransformer();
  GeometryConverter geometryConverter = new GeometryConverter();
  CityJsonTextureComputer textureComputerMock = mock(CityJsonTextureComputer.class);
  SwissBoundaryChecker swissBoundaryCheckerMock = mock(SwissBoundaryChecker.class);
  Roofer3DBagCityJSONValidator roofer3DBagCityJSONValidatorMock =
      mock(Roofer3DBagCityJSONValidator.class);
  CityJSON3DBagRooferProcessor subject =
      new CityJSON3DBagRooferProcessor(
          bucketComponentMock,
          featureMapperMock,
          lidarApiFacadeMock,
          roofer3DBagApiClientMock,
          fileWriterMock,
          coordinateTransformer,
          geometryConverter,
          textureComputerMock,
          swissBoundaryCheckerMock,
          roofer3DBagCityJSONValidatorMock);

  @BeforeEach
  void setup() {
    doNothing().when(roofer3DBagCityJSONValidatorMock).accept(any());
  }

  @SneakyThrows
  @Test
  void convert_request_to_city_json_from_3d_bag_roofer() {
    var cityJSONRequestMock = mock(CityJSONRequest.class);
    var geoJsonBuildingPresignedURLMock = mock(URL.class);
    var geometryMock = mock(Geometry.class);
    var delimitationFeature =
        new Feature()
            .type(FEATURE)
            .geometry(
                new FeatureGeometry(new MultiPolygon().coordinates(multiPolygonCoordinates())))
            .properties(Map.of("id", randomUUID().toString()));
    var cityJsonGenerationResponseMock = mock(CityJsonGenerationResponse.class);
    var rooferCityJsonURLMock = mock(URL.class);
    var lidarUrl = "http://lidar/" + randomUUID();
    var geoJsonUrl = "http://geojson/" + randomUUID();
    var cityJsonUrl = "http://cityJson/" + randomUUID();

    when(cityJSONRequestMock.getRequestDelimitations())
        .thenReturn(List.of(toDomainFeature(delimitationFeature)));
    when((rooferCityJsonURLMock.openStream()))
        .thenReturn(new ClassPathResource("/cityjson/dummy.jsonl").getInputStream());
    when(cityJSONRequestMock.getComplexityFactor()).thenReturn(null);
    when(cityJSONRequestMock.getKnn()).thenReturn(null);
    when(geoJsonBuildingPresignedURLMock.toString()).thenReturn(geoJsonUrl);
    when(bucketComponentMock.upload(any(), any())).thenReturn(mock());
    when(bucketComponentMock.presign(any(), any())).thenReturn(geoJsonBuildingPresignedURLMock);
    when(featureMapperMock.domainToGeometryWithMultipolygonHandler(any())).thenReturn(geometryMock);
    when(lidarApiFacadeMock.getUniqueLidarFilesUrls(Collections.singleton(geometryMock)))
        .thenReturn(Map.of(lidarUrl, Set.of(geometryMock)));
    when(cityJsonGenerationResponseMock.getCityJsonUrl()).thenReturn(cityJsonUrl);
    when(roofer3DBagApiClientMock.generateCityJson(
            CityJsonGenerationRequest.builder()
                .geoJsonBuildingPresignedUrl(geoJsonUrl)
                .lidarPresignedUrls(List.of(lidarUrl))
                .build(),
            null,
            null))
        .thenReturn(cityJsonGenerationResponseMock);
    MockedConstruction<URI> mockedUri =
        Mockito.mockConstruction(
            URI.class,
            (mock, ctx) -> {
              Mockito.when(mock.toURL()).thenReturn(rooferCityJsonURLMock);
            });
    when(textureComputerMock.applyTexture(any(), any()))
        .thenAnswer(invocation -> invocation.getArgument(1));

    var cityJsonDocMock = mock(CityJsonIO.Doc.class);
    MockedStatic<CityJsonIO> cityJsonIOMockedStatic = mockStatic(CityJsonIO.class);
    cityJsonIOMockedStatic
        .when(() -> computeAdditionalProperties(any()))
        .thenReturn(cityJsonDocMock);
    cityJsonIOMockedStatic
        .when(() -> write(eq(cityJsonDocMock), any()))
        .thenAnswer(invocation -> null);

    var actual = subject.apply(cityJSONRequestMock);

    assertEquals(1, actual.size());
    var actualCityJSON = actual.getFirst();
    assertNotNull(actualCityJSON.getId());
    assertNotNull(actualCityJSON.getS3FileKey());
    assertTrue(
        actual.contains(
            CityJSON.builder()
                .id(actualCityJSON.getId())
                .s3FileKey(actualCityJSON.getS3FileKey())
                .request(cityJSONRequestMock)
                .build()));
    mockedUri.close();
    cityJsonIOMockedStatic.close();
  }

  private List<List<List<List<BigDecimal>>>> multiPolygonCoordinates() {
    return List.of(
        List.of(
            List.of(
                List.of(new BigDecimal("2.3522"), new BigDecimal("48.8566")), // Paris
                List.of(new BigDecimal("2.3622"), new BigDecimal("48.8566")),
                List.of(new BigDecimal("2.3622"), new BigDecimal("48.8666")),
                List.of(new BigDecimal("2.3522"), new BigDecimal("48.8666")),
                List.of(new BigDecimal("2.3522"), new BigDecimal("48.8566")) // Fermeture
                )),
        List.of(
            List.of(
                List.of(new BigDecimal("5.3698"), new BigDecimal("43.2965")), // Marseille
                List.of(new BigDecimal("5.3798"), new BigDecimal("43.2965")),
                List.of(new BigDecimal("5.3748"), new BigDecimal("43.3065")),
                List.of(new BigDecimal("5.3698"), new BigDecimal("43.2965")) // Fermeture
                )));
  }
}
