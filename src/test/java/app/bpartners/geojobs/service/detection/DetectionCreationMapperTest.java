package app.bpartners.geojobs.service.detection;

import static app.bpartners.geojobs.endpoint.rest.model.ModelName.REVETEMENT_OBSTACLE;
import static app.bpartners.geojobs.endpoint.rest.model.ModelName.TOITURE;
import static app.bpartners.geojobs.endpoint.rest.model.ModelName.VEGETATION;
import static app.bpartners.geojobs.repository.model.detection.DetectableType.CHEMINEE;
import static app.bpartners.geojobs.repository.model.detection.DetectableType.LUMINEUX;
import static app.bpartners.geojobs.repository.model.detection.DetectableType.MOISISSURE;
import static app.bpartners.geojobs.repository.model.detection.DetectableType.OBSTACLE;
import static app.bpartners.geojobs.repository.model.detection.DetectableType.PANNEAU_PHOTOVOLTAIQUE;
import static app.bpartners.geojobs.repository.model.detection.DetectableType.TOITURE_REVETEMENT;
import static app.bpartners.geojobs.repository.model.detection.DetectableType.USURE;
import static app.bpartners.geojobs.repository.model.detection.DetectableType.VELUX;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.bpartners.geojobs.endpoint.rest.controller.v1.mapper.DetectableObjectTypeMapper;
import app.bpartners.geojobs.endpoint.rest.model.CreateDetection;
import app.bpartners.geojobs.endpoint.rest.model.DetectableObjectModel;
import app.bpartners.geojobs.endpoint.rest.model.Feature;
import app.bpartners.geojobs.endpoint.rest.model.FeatureGeometry;
import app.bpartners.geojobs.endpoint.rest.model.GeoServerParameter;
import app.bpartners.geojobs.endpoint.rest.model.GeoServerProperties;
import app.bpartners.geojobs.endpoint.rest.model.ModelName;
import app.bpartners.geojobs.endpoint.rest.model.Point;
import app.bpartners.geojobs.endpoint.rest.model.Polygon;
import app.bpartners.geojobs.model.exception.ApiException;
import app.bpartners.geojobs.model.exception.BadRequestException;
import app.bpartners.geojobs.repository.model.detection.DetectableObjectConfiguration;
import app.bpartners.geojobs.repository.model.detection.DetectableType;
import app.bpartners.geojobs.repository.model.detection.Detection;
import app.bpartners.geojobs.service.BuildingFinder;
import app.bpartners.geojobs.service.detection.inference.InferenceModelCardConf;
import app.bpartners.geojobs.service.detection.inference.ModelCardMapping;
import app.bpartners.geojobs.service.detection.inference.ModelCardRegistry;
import app.bpartners.geojobs.service.detection.inference.ModelCardResolver;
import app.bpartners.geojobs.service.geodata.MapLayerResolver;
import app.bpartners.geojobs.service.geodata.model.GeodataMapLayer;
import app.bpartners.geojobs.service.geojson.GeometryConverter;
import app.bpartners.geojobs.service.geoserver.GeoServerConfiguration;
import app.bpartners.geojobs.validator.FeatureTypeChecker;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class DetectionCreationMapperTest {

  DetectableObjectTypeMapper detectableObjectTypeMapper = new DetectableObjectTypeMapper();
  FeatureTypeChecker featureTypeChecker = new FeatureTypeChecker();
  GeometryConverter geometryConverterMock = mock();
  BuildingFinder buildingFinderMock = mock();
  MapLayerResolver mapLayerResolverMock = mock();
  GeoServerConfiguration geoServerConfigurationMock = mock();
  ModelCardResolver modelCardResolverMock = mock();

  DetectionCreationMapper subject =
      new DetectionCreationMapper(
          detectableObjectTypeMapper,
          featureTypeChecker,
          mapLayerResolverMock,
          modelCardResolverMock,
          geoServerConfigurationMock,
          geometryConverterMock,
          buildingFinderMock,
          new DefaultResolutionModelValidator());

  @Test
  void map_detection_with_detectable_object_model_list_ok() {
    var detectionE2Id = randomUUID().toString();
    var communityOwnerId = randomUUID().toString();
    var createDetection = createDetection();

    var actual = subject.apply(createDetection, detectionE2Id, communityOwnerId, false, null);

    assertTrue(actual.getDetectableObjectModelList().containsAll(detectableObjectModelList()));
    assertFalse(actual.getDetectableObjectConfigurations().isEmpty()); // TODO
  }

  @Test
  void map_detection_with_no_detectable_object_model_list_ok() {
    var detectionE2Id = randomUUID().toString();
    var communityOwnerId = randomUUID().toString();
    var createDetection = createDetection().detectableObjectModelList(null);

    var actual = subject.apply(createDetection, detectionE2Id, communityOwnerId, false, null);

    assertNotNull(actual.getDetectableObjectModel());
    assertFalse(actual.getDetectableObjectConfigurations().isEmpty());
  }

  @Test
  void refuses_a_layer_whose_precision_is_not_supported_by_every_object() {
    givenLayerWithPrecision(10);
    when(modelCardResolverMock.supportedSpatialResolutionsInCm(any()))
        .thenReturn(new LinkedHashSet<>(List.of(5.0, 20.0)));

    var exception =
        assertThrows(
            ApiException.class,
            () -> subject.apply(pointDetection(), randomUUID().toString(), null, false, null));

    assertTrue(exception.getMessage().contains("image resolution must be 5 or 20 cm"));
    assertTrue(exception.getMessage().contains("precisionLevelInCm = 10"));
  }

  @Test
  void refuses_a_layer_when_no_resolution_is_common_to_every_object() {
    givenLayerWithPrecision(5);
    when(modelCardResolverMock.supportedSpatialResolutionsInCm(any())).thenReturn(Set.of());

    var exception =
        assertThrows(
            ApiException.class,
            () -> subject.apply(pointDetection(), randomUUID().toString(), null, false, null));

    assertTrue(
        exception.getMessage().contains("no image resolution is supported by all the requested"));
  }

  @Test
  void accepts_a_layer_matching_one_of_the_supported_resolutions() {
    givenLayerWithPrecision(20);
    when(modelCardResolverMock.supportedSpatialResolutionsInCm(any()))
        .thenReturn(new LinkedHashSet<>(List.of(5.0, 20.0)));

    assertDoesNotThrow(
        () -> subject.apply(pointDetection(), randomUUID().toString(), null, false, null));

    verify(geoServerConfigurationMock).defaultGeoServerProperties("layer", 20);
  }

  @Test
  void refuses_a_layer_without_precision() {
    givenLayerWithPrecision(null);
    when(modelCardResolverMock.supportedSpatialResolutionsInCm(any())).thenReturn(Set.of(5.0));

    assertThrows(
        ApiException.class,
        () -> subject.apply(pointDetection(), randomUUID().toString(), null, false, null));
  }

  @Test
  void checks_the_precision_of_provided_geo_server_properties() {
    when(modelCardResolverMock.supportedSpatialResolutionsInCm(any())).thenReturn(Set.of(5.0));

    var exception =
        assertThrows(
            ApiException.class,
            () ->
                subject.apply(
                    pointDetection(providedGeoServerProperties(20)),
                    randomUUID().toString(),
                    null,
                    false,
                    null));

    assertTrue(exception.getMessage().contains("image resolution must be 5 cm"));
    assertTrue(exception.getMessage().contains("precisionLevelInCm = 20"));
  }

  @Test
  void does_not_check_provided_geo_server_properties_without_precision_on_an_unknown_layer() {
    givenLayerWithPrecision(20);

    var actual =
        subject.apply(
            pointDetection(providedGeoServerProperties(null)),
            randomUUID().toString(),
            null,
            false,
            null);

    assertFalse(actual.getDetectableObjectConfigurations().isEmpty());
    assertNull(actual.spatialResolutionInCm());
    verify(modelCardResolverMock, never()).supportedSpatialResolutionsInCm(any());
  }

  @Test
  void deduces_the_missing_precision_of_provided_geo_server_properties_from_the_same_layer() {
    givenLayerWithPrecision(20);
    when(modelCardResolverMock.supportedSpatialResolutionsInCm(any()))
        .thenReturn(new LinkedHashSet<>(List.of(5.0, 20.0)));

    var actual =
        subject.apply(
            pointDetection(providedGeoServerProperties("layer", null)),
            randomUUID().toString(),
            null,
            false,
            null);

    assertEquals(20.0, actual.spatialResolutionInCm());
    verify(modelCardResolverMock).supportedSpatialResolutionsInCm(any());
  }

  @Test
  void refuses_provided_geo_server_properties_whose_deduced_precision_is_not_supported() {
    givenLayerWithPrecision(20);
    when(modelCardResolverMock.supportedSpatialResolutionsInCm(any())).thenReturn(Set.of(5.0));

    var exception =
        assertThrows(
            ApiException.class,
            () ->
                subject.apply(
                    pointDetection(providedGeoServerProperties("layer", null)),
                    randomUUID().toString(),
                    null,
                    false,
                    null));

    assertTrue(exception.getMessage().contains("image resolution must be 5 cm"));
    assertTrue(exception.getMessage().contains("precisionLevelInCm = 20"));
  }

  @Test
  void refuses_toiture_model_on_provided_20_cm_geo_server_properties_whatever_the_model_cards() {
    when(modelCardResolverMock.supportedSpatialResolutionsInCm(any()))
        .thenReturn(new LinkedHashSet<>(List.of(5.0, 20.0)));

    var exception =
        assertThrows(
            ApiException.class,
            () ->
                subject.apply(
                    polygonDetection(
                        TOITURE, providedGeoServerProperties("FLUX_IGN_2025_20CM", 20)),
                    randomUUID().toString(),
                    null,
                    false,
                    null));

    assertEquals(
        "Cannot process detection : image resolution must be 5 cm. Current layer ="
            + " FLUX_IGN_2025_20CM (precisionLevelInCm = 20). Model = BP_TOITURE",
        exception.getMessage());
  }

  @Test
  void refuses_toiture_model_on_a_resolved_20_cm_layer_whatever_the_model_cards() {
    givenLayerWithPrecision(20);
    when(modelCardResolverMock.supportedSpatialResolutionsInCm(any()))
        .thenReturn(new LinkedHashSet<>(List.of(5.0, 20.0)));

    var exception =
        assertThrows(
            ApiException.class,
            () ->
                subject.apply(
                    polygonDetection(TOITURE), randomUUID().toString(), null, false, null));

    assertTrue(exception.getMessage().contains("image resolution must be 5 cm"));
    assertTrue(exception.getMessage().contains("precisionLevelInCm = 20"));
    assertTrue(exception.getMessage().endsWith("Model = BP_TOITURE"));
  }

  @Test
  void refuses_toiture_model_on_provided_layer_whose_precision_cannot_be_deduced() {
    givenLayerWithPrecision(5);

    var exception =
        assertThrows(
            ApiException.class,
            () ->
                subject.apply(
                    polygonDetection(TOITURE, providedGeoServerProperties("other-layer", null)),
                    randomUUID().toString(),
                    null,
                    false,
                    null));

    assertEquals(
        "Cannot process detection : image resolution must be 5 cm. Current layer = other-layer"
            + " (precisionLevelInCm = null). Model = BP_TOITURE",
        exception.getMessage());
  }

  @Test
  void refuses_toiture_model_on_provided_layer_without_any_imagery_layer_at_position() {
    when(mapLayerResolverMock.apply(anyDouble(), anyDouble())).thenReturn(Optional.empty());

    assertThrows(
        ApiException.class,
        () ->
            subject.apply(
                polygonDetection(TOITURE, providedGeoServerProperties("layer", null)),
                randomUUID().toString(),
                null,
                false,
                null));
  }

  @Test
  void accepts_toiture_model_on_provided_layer_whose_deduced_precision_is_5_cm() {
    givenLayerWithPrecision(5);
    when(modelCardResolverMock.supportedSpatialResolutionsInCm(any())).thenReturn(Set.of(5.0));

    var actual =
        subject.apply(
            polygonDetection(TOITURE, providedGeoServerProperties("layer", null)),
            randomUUID().toString(),
            null,
            false,
            null);

    assertEquals(5.0, actual.spatialResolutionInCm());
  }

  @Test
  void accepts_toiture_model_on_provided_5_cm_geo_server_properties() {
    when(modelCardResolverMock.supportedSpatialResolutionsInCm(any())).thenReturn(Set.of(5.0));

    assertDoesNotThrow(
        () ->
            subject.apply(
                polygonDetection(TOITURE, providedGeoServerProperties("layer-5cm", 5)),
                randomUUID().toString(),
                null,
                false,
                null));
  }

  @Test
  void accepts_a_20_cm_layer_for_a_model_other_than_toiture_when_provided() {
    when(modelCardResolverMock.supportedSpatialResolutionsInCm(any()))
        .thenReturn(new LinkedHashSet<>(List.of(5.0, 20.0)));

    assertDoesNotThrow(
        () ->
            subject.apply(
                polygonDetection(
                    REVETEMENT_OBSTACLE, providedGeoServerProperties("layer-20cm", 20)),
                randomUUID().toString(),
                null,
                false,
                null));
  }

  @Test
  void roof_obstacle_model_gets_lumineux_only_on_a_20_cm_layer() {
    var realResolver = new ModelCardResolver(roofObstacleConf(), mock(ModelCardRegistry.class));
    var realSubject = subjectWith(realResolver);

    givenLayerWithPrecision(20);
    var at20cm =
        realSubject.apply(roofObstaclePointDetection(), randomUUID().toString(), null, false, null);
    givenLayerWithPrecision(5);
    var at5cm =
        realSubject.apply(roofObstaclePointDetection(), randomUUID().toString(), null, false, null);

    assertEquals(
        Set.of(OBSTACLE, CHEMINEE, VELUX, PANNEAU_PHOTOVOLTAIQUE, LUMINEUX), typesOf(at20cm));
    assertEquals(Set.of(OBSTACLE, CHEMINEE, VELUX, PANNEAU_PHOTOVOLTAIQUE), typesOf(at5cm));
  }

  @Test
  void roof_obstacle_model_is_refused_on_a_10_cm_layer() {
    var realSubject =
        subjectWith(new ModelCardResolver(roofObstacleConf(), mock(ModelCardRegistry.class)));
    givenLayerWithPrecision(10);

    assertThrows(
        ApiException.class,
        () ->
            realSubject.apply(
                roofObstaclePointDetection(), randomUUID().toString(), null, false, null));
  }

  @Test
  void toiture_model_requires_a_5_cm_layer() {
    var realSubject =
        subjectWith(new ModelCardResolver(roofObstacleConf(), mock(ModelCardRegistry.class)));
    givenLayerWithPrecision(20);

    var exception =
        assertThrows(
            ApiException.class,
            () ->
                realSubject.apply(
                    toiturePointDetection(), randomUUID().toString(), null, false, null));
    assertTrue(exception.getMessage().contains("image resolution must be 5 cm"));

    givenLayerWithPrecision(5);
    assertDoesNotThrow(
        () ->
            realSubject.apply(toiturePointDetection(), randomUUID().toString(), null, false, null));
  }

  @Test
  void refuses_a_position_without_any_imagery_layer() {
    when(mapLayerResolverMock.apply(anyDouble(), anyDouble())).thenReturn(Optional.empty());

    assertThrows(
        BadRequestException.class,
        () -> subject.apply(pointDetection(), randomUUID().toString(), null, false, null));
  }

  private void givenLayerWithPrecision(Integer precisionInCm) {
    when(mapLayerResolverMock.apply(anyDouble(), anyDouble()))
        .thenReturn(
            Optional.of(
                new GeodataMapLayer("id", "layer", 2024, "GEOSERVER", null, null, precisionInCm)));
  }

  private CreateDetection pointDetection() {
    return pointDetection(VEGETATION);
  }

  private CreateDetection pointDetection(ModelName modelName) {
    var point = new Point().coordinates(List.of(BigDecimal.valueOf(2.3), BigDecimal.valueOf(48.8)));
    point.setType(Point.TypeEnum.POINT);
    return new CreateDetection()
        .detectableObjectModel(new DetectableObjectModel().modelName(modelName))
        .emailReceiver("<EMAIL>")
        .geoJsonZone(List.of(new Feature().geometry(new FeatureGeometry(point))));
  }

  private CreateDetection pointDetection(GeoServerProperties geoServerProperties) {
    return pointDetection().geoServerProperties(geoServerProperties);
  }

  private CreateDetection polygonDetection(
      ModelName modelName, GeoServerProperties geoServerProperties) {
    return polygonDetection(modelName).geoServerProperties(geoServerProperties);
  }

  private static GeoServerProperties providedGeoServerProperties(Integer precisionLevelInCm) {
    return providedGeoServerProperties("provided-layer", precisionLevelInCm);
  }

  private static GeoServerProperties providedGeoServerProperties(
      String layer, Integer precisionLevelInCm) {
    return new GeoServerProperties()
        .geoServerParameter(
            new GeoServerParameter().layers(layer).precisionLevelInCm(precisionLevelInCm));
  }

  private CreateDetection roofObstaclePointDetection() {
    return polygonDetection(REVETEMENT_OBSTACLE);
  }

  private CreateDetection toiturePointDetection() {
    return polygonDetection(TOITURE);
  }

  private CreateDetection polygonDetection(ModelName modelName) {
    var ring =
        List.of(
            List.of(BigDecimal.valueOf(2.3), BigDecimal.valueOf(48.8)),
            List.of(BigDecimal.valueOf(2.4), BigDecimal.valueOf(48.8)),
            List.of(BigDecimal.valueOf(2.4), BigDecimal.valueOf(48.9)),
            List.of(BigDecimal.valueOf(2.3), BigDecimal.valueOf(48.8)));
    var polygon = new Polygon().coordinates(List.of(ring));
    polygon.setType(Polygon.TypeEnum.POLYGON);
    return new CreateDetection()
        .detectableObjectModel(new DetectableObjectModel().modelName(modelName))
        .emailReceiver("<EMAIL>")
        .geoJsonZone(List.of(new Feature().geometry(new FeatureGeometry(polygon))));
  }

  private DetectionCreationMapper subjectWith(ModelCardResolver resolver) {
    return new DetectionCreationMapper(
        detectableObjectTypeMapper,
        featureTypeChecker,
        mapLayerResolverMock,
        resolver,
        geoServerConfigurationMock,
        geometryConverterMock,
        buildingFinderMock,
        new DefaultResolutionModelValidator());
  }

  private static InferenceModelCardConf roofObstacleConf() {
    var conf = mock(InferenceModelCardConf.class);
    var mappings = new ArrayList<ModelCardMapping>();
    List.of(OBSTACLE, CHEMINEE, VELUX, PANNEAU_PHOTOVOLTAIQUE)
        .forEach(
            type -> {
              mappings.add(new ModelCardMapping(type, "obstacles-5cm", 21, 5.0));
              mappings.add(new ModelCardMapping(type, "obstacles-20cm", 19, 20.0));
            });
    mappings.add(new ModelCardMapping(LUMINEUX, "obstacles-20cm", 19, 20.0));
    mappings.add(new ModelCardMapping(TOITURE_REVETEMENT, "damages-5cm", 21, 5.0));
    mappings.add(new ModelCardMapping(USURE, "damages-5cm", 21, 5.0));
    mappings.add(new ModelCardMapping(MOISISSURE, "damages-5cm", 21, 5.0));
    when(conf.getMappings()).thenReturn(mappings);
    return conf;
  }

  private static Set<DetectableType> typesOf(Detection detection) {
    return detection.getDetectableObjectConfigurations().stream()
        .map(DetectableObjectConfiguration::getObjectType)
        .collect(Collectors.toSet());
  }

  private CreateDetection createDetection() {
    return new CreateDetection()
        .detectableObjectModel(detectableObjectModel())
        .detectableObjectModelList(detectableObjectModelList())
        .emailReceiver("<EMAIL>");
  }

  private List<DetectableObjectModel> detectableObjectModelList() {
    return List.of(
        new DetectableObjectModel().modelName(TOITURE),
        new DetectableObjectModel().modelName(VEGETATION));
  }

  private DetectableObjectModel detectableObjectModel() {
    return new DetectableObjectModel().modelName(TOITURE);
  }
}
