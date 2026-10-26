package app.bpartners.geojobs.service.detection;

import static app.bpartners.geojobs.repository.model.detection.DetectableType.TOITURE_REVETEMENT;
import static app.bpartners.geojobs.repository.model.detection.RoofCoveringType.ROOF_ARDOISE;
import static app.bpartners.geojobs.repository.model.detection.RoofCoveringType.ROOF_TUILES;
import static java.util.UUID.randomUUID;
import static org.apache.commons.io.FileUtils.readFileToByteArray;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import app.bpartners.geojobs.endpoint.rest.model.TileCoordinates;
import app.bpartners.geojobs.file.bucket.BucketConf;
import app.bpartners.geojobs.file.bucket.CustomBucketComponent;
import app.bpartners.geojobs.repository.model.detection.RoofCoveringType;
import app.bpartners.geojobs.repository.model.tiling.Tile;
import app.bpartners.geojobs.service.detection.inference.DeployedModelCard;
import app.bpartners.geojobs.service.detection.inference.InferenceApiClient;
import app.bpartners.geojobs.service.detection.inference.InferenceModelCardConf;
import app.bpartners.geojobs.service.detection.inference.InferenceRequest;
import app.bpartners.geojobs.service.detection.inference.ModelCardMapping;
import app.bpartners.geojobs.service.detection.inference.ModelCardRegistry;
import app.bpartners.geojobs.service.detection.inference.ModelCardResolver;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.imageio.ImageIO;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

class RoofCoveringDetectorTest {
  private static final String MOCK_DETECTION_API_URL = "https://detection.api";
  ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
  RestTemplate restTemplateMock = mock();
  CustomBucketComponent bucketComponentMock = mock();
  InferenceApiClient inferenceApiClientMock = mock();
  InferenceModelCardConf inferenceModelCardConfMock = mock();
  ModelCardRegistry modelCardRegistryMock = mock();
  RoofCoveringDetector subject =
      new RoofCoveringDetector(
          objectMapper,
          restTemplateMock,
          MOCK_DETECTION_API_URL,
          bucketComponentMock,
          inferenceApiClientMock,
          new ModelCardResolver(inferenceModelCardConfMock, modelCardRegistryMock));

  @SneakyThrows
  @Test
  void retrieve_covering_response() {
    var expected = getRoofCoveringDetectionBody();
    var maskFile =
        new ClassPathResource("images/ALPES-MARITIMES_2024_5cm/20/544681/383095.jpg").getFile();
    var imageFile =
        new ClassPathResource("images/ALPES-MARITIMES_2024_5cm/20/544680/383095.jpg").getFile();
    var tileMock = mock(Tile.class);
    var bucketConfMock = mock(BucketConf.class);
    var tileBucketPath = "dummyPath";
    var dummyBucketName = "dummyBucketName";
    var detectionE2Id = randomUUID().toString();

    when(tileMock.getDetectionE2Id()).thenReturn(detectionE2Id);
    when(tileMock.getBucketPath()).thenReturn(tileBucketPath);
    when(bucketConfMock.getBucketName()).thenReturn(dummyBucketName);
    when(bucketComponentMock.getBucketConf()).thenReturn(bucketConfMock);
    when(bucketComponentMock.download(dummyBucketName, tileBucketPath)).thenReturn(imageFile);
    when(restTemplateMock.postForEntity(
            any(String.class), any(), eq(RoofCoveringDetector.RoofCoveringDetectionResponse.class)))
        .thenReturn(new ResponseEntity<>(expected, HttpStatus.OK));

    var actual = subject.apply(tileMock, maskFile, null);

    var requestCaptor = ArgumentCaptor.forClass(HttpEntity.class);
    verify(restTemplateMock)
        .postForEntity(
            eq(MOCK_DETECTION_API_URL + "/coatings"),
            requestCaptor.capture(),
            eq(RoofCoveringDetector.RoofCoveringDetectionResponse.class));
    var actualRequest = requestCaptor.getValue();
    var actualPayloadStringValue = (String) actualRequest.getBody();
    var actualRequestPayload =
        objectMapper.readValue(
            actualPayloadStringValue, RoofCoveringDetector.RoofCoveringDetectionPayload.class);
    var expectedPayload =
        new RoofCoveringDetector.RoofCoveringDetectionPayload(
            encodeBase64(imageFile), encodeBase64(maskFile), tileBucketPath, detectionE2Id);
    assertEquals(expected, actual);
    assertEquals(expectedPayload, actualRequestPayload);
  }

  @SneakyThrows
  @Test
  void invoke_the_5cm_model_card_when_the_image_precision_is_5cm() {
    var imageFile = tileImageFile();
    var maskFile = tileMaskFile();
    mockInference(imageFile);
    when(inferenceApiClientMock.infer(any(InferenceRequest.class))).thenReturn(inferenceResponse());

    var actual = subject.apply(tileAtZoom(21), maskFile, 5.0);

    var requestCaptor = ArgumentCaptor.forClass(InferenceRequest.class);
    verify(inferenceApiClientMock).infer(requestCaptor.capture());
    verifyNoInteractions(restTemplateMock);
    var request = requestCaptor.getValue();
    assertEquals("bp-toiture-revetements-v2.0", request.modelCardId());
    assertEquals(21, request.zoomLevel());
    assertEquals(5.0, request.spatialResolution());
    assertEquals(encodeBase64(imageFile), request.image1());
    assertEquals(encodeBase64(maskFile), request.mask1());
    assertEquals(
        new RoofCoveringDetector.RoofCoveringDetectionResponse(
            new RoofCovering(ROOF_TUILES, 10000L), new RoofCovering(ROOF_ARDOISE, 100L)),
        actual);
  }

  @SneakyThrows
  @Test
  void invoke_the_20cm_model_card_when_the_image_precision_is_20cm() {
    mockInference(tileImageFile());
    when(inferenceApiClientMock.infer(any(InferenceRequest.class))).thenReturn(inferenceResponse());

    subject.apply(tileAtZoom(19), tileMaskFile(), 20.0);

    var requestCaptor = ArgumentCaptor.forClass(InferenceRequest.class);
    verify(inferenceApiClientMock).infer(requestCaptor.capture());
    verifyNoInteractions(restTemplateMock);
    assertEquals("revets-fp-v1", requestCaptor.getValue().modelCardId());
    assertEquals(19, requestCaptor.getValue().zoomLevel());
    assertEquals(20.0, requestCaptor.getValue().spatialResolution());
  }

  @SneakyThrows
  @Test
  void return_null_when_the_inference_api_fails() {
    mockInference(tileImageFile());
    when(inferenceApiClientMock.infer(any(InferenceRequest.class)))
        .thenThrow(new IllegalStateException("inference failure"));

    assertNull(subject.apply(tileAtZoom(21), tileMaskFile(), 5.0));
    verifyNoInteractions(restTemplateMock);
  }

  @SneakyThrows
  @Test
  void return_null_when_the_inference_api_detects_no_known_covering() {
    mockInference(tileImageFile());
    when(inferenceApiClientMock.infer(any(InferenceRequest.class)))
        .thenReturn(
            inferenceResponse(
                Map.of("0", region("background", 0, 50), "1", region("roof_x", 0, 5))));

    assertNull(subject.apply(tileAtZoom(21), tileMaskFile(), 5.0));
  }

  @SneakyThrows
  @ParameterizedTest
  @ValueSource(
      strings = {
        "roof_ardoise",
        "roof_asphalte_bitume",
        "roof_bac_acier",
        "roof_beton_brut",
        "roof_fibro_ciment",
        "roof_gravier",
        "roof_membrane_synthetique",
        "roof_tole_ondulee",
        "roof_tuiles",
        "roof_zinc"
      })
  void map_every_class_of_the_toiture_revetements_model_card(String modelClass) {
    mockInference(tileImageFile());
    when(inferenceApiClientMock.infer(any(InferenceRequest.class)))
        .thenReturn(inferenceResponse(Map.of("0", region(modelClass, 0, 10))));

    var actual = subject.apply(tileAtZoom(21), tileMaskFile(), 5.0);

    assertNotNull(actual);
    assertEquals(RoofCoveringType.valueOf(modelClass.toUpperCase()), actual.primary().coating());
    assertNull(actual.secondary());
  }

  @SneakyThrows
  @ParameterizedTest
  @CsvSource({
    "roof_tuiles,ROOF_TUILES",
    "roof_ardoise,ROOF_ARDOISE",
    "zinc,ROOF_ZINC",
    "tole_ondulee,ROOF_TOLE_ONDULEE",
    "bac_acier,ROOF_BAC_ACIER",
    "asphalte_bitume,ROOF_ASPHALTE_BITUME",
    "fibre_ciment,ROOF_FIBRO_CIMENT",
    "beton_brut,ROOF_BETON_BRUT"
  })
  void map_every_class_of_the_revets_fp_model_card(String modelClass, RoofCoveringType expected) {
    mockInference(tileImageFile());
    when(inferenceApiClientMock.infer(any(InferenceRequest.class)))
        .thenReturn(inferenceResponse(Map.of("0", region(modelClass, 0, 10))));

    var actual = subject.apply(tileAtZoom(19), tileMaskFile(), 20.0);

    assertNotNull(actual);
    assertEquals(expected, actual.primary().coating());
  }

  @Test
  void roof_covering_type_has_exactly_the_classes_of_the_toiture_revetements_model_card() {
    assertEquals(10, RoofCoveringType.values().length);
  }

  @SneakyThrows
  @Test
  void fall_back_to_the_coatings_api_when_no_model_card_matches_the_image_precision() {
    mockInference(tileImageFile());
    when(restTemplateMock.postForEntity(
            any(String.class), any(), eq(RoofCoveringDetector.RoofCoveringDetectionResponse.class)))
        .thenReturn(new ResponseEntity<>(getRoofCoveringDetectionBody(), HttpStatus.OK));

    var actual = subject.apply(tileAtZoom(21), tileMaskFile(), 10.0);

    verifyNoInteractions(inferenceApiClientMock);
    assertEquals(getRoofCoveringDetectionBody(), actual);
  }

  @SneakyThrows
  private void mockInference(File imageFile) {
    var image = ImageIO.read(imageFile);
    var modelCardConf =
        List.of(
            new ModelCardMapping(TOITURE_REVETEMENT, "bp-toiture-revetements-v2.0", 21, 5.0),
            new ModelCardMapping(TOITURE_REVETEMENT, "revets-fp-v1", 19, 20.0));
    when(inferenceModelCardConfMock.getMappings()).thenReturn(modelCardConf);
    modelCardConf.forEach(
        mapping ->
            when(modelCardRegistryMock.findById(mapping.modelCardId()))
                .thenReturn(
                    Optional.of(
                        new DeployedModelCard(
                            mapping.modelCardId(),
                            DeployedModelCard.ProblemType.SEMANTIC_SEGMENTATION,
                            1,
                            true,
                            image.getWidth(),
                            image.getHeight(),
                            true))));
    var bucketConfMock = mock(BucketConf.class);
    when(bucketConfMock.getBucketName()).thenReturn("dummyBucketName");
    when(bucketComponentMock.getBucketConf()).thenReturn(bucketConfMock);
    when(bucketComponentMock.download("dummyBucketName", "dummyPath")).thenReturn(imageFile);
  }

  private static Tile tileAtZoom(int zoom) {
    return Tile.builder()
        .id(randomUUID().toString())
        .bucketPath("dummyPath")
        .coordinates(new TileCoordinates().x(544680).y(383095).z(zoom))
        .build();
  }

  @SneakyThrows
  private static File tileImageFile() {
    return new ClassPathResource("images/ALPES-MARITIMES_2024_5cm/20/544680/383095.jpg").getFile();
  }

  @SneakyThrows
  private static File tileMaskFile() {
    return new ClassPathResource("images/ALPES-MARITIMES_2024_5cm/20/544681/383095.jpg").getFile();
  }

  private static DetectionResponseV2 inferenceResponse() {
    return inferenceResponse(
        Map.of(
            "0", region("roof_ardoise", 0, 10),
            "1", region("roof_tuiles", 0, 100),
            "2", region("not_a_covering", 0, 200)));
  }

  private static DetectionResponseV2 inferenceResponse(
      Map<String, DetectionResponseV2.ImageData.Region> regions) {
    return DetectionResponseV2.builder()
        .images(
            Map.of("tile.jpg", DetectionResponseV2.ImageData.builder().regions(regions).build()))
        .build();
  }

  private static DetectionResponseV2.ImageData.Region region(String label, int min, int max) {
    return DetectionResponseV2.ImageData.Region.builder()
        .regionAttributes(Map.of("label", label))
        .shapeAttributes(
            DetectionResponseV2.ImageData.ShapeAttributes.builder()
                .name("polygon")
                .allPointsX(points(min, max, max, min))
                .allPointsY(points(min, min, max, max))
                .build())
        .build();
  }

  private static List<BigDecimal> points(int... values) {
    return Arrays.stream(values).mapToObj(BigDecimal::new).toList();
  }

  private RoofCoveringDetector.RoofCoveringDetectionResponse getRoofCoveringDetectionBody() {
    return new RoofCoveringDetector.RoofCoveringDetectionResponse(
        new RoofCovering(ROOF_ARDOISE, 1343L), new RoofCovering(ROOF_TUILES, 1000L));
  }

  @SneakyThrows
  private String encodeBase64(File image) {
    return Base64.getEncoder().encodeToString(readFileToByteArray(image));
  }
}
