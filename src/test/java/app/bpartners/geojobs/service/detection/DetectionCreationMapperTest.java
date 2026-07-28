package app.bpartners.geojobs.service.detection;

import static app.bpartners.geojobs.endpoint.rest.model.ModelName.TOITURE;
import static app.bpartners.geojobs.endpoint.rest.model.ModelName.VEGETATION;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

import app.bpartners.geojobs.endpoint.rest.controller.v1.mapper.DetectableObjectTypeMapper;
import app.bpartners.geojobs.endpoint.rest.model.CreateDetection;
import app.bpartners.geojobs.endpoint.rest.model.DetectableObjectModel;
import app.bpartners.geojobs.repository.CommunityAuthorizationRepository;
import app.bpartners.geojobs.service.BuildingFinder;
import app.bpartners.geojobs.service.dashboard.AreaPictureApi;
import app.bpartners.geojobs.service.geojson.GeometryConverter;
import app.bpartners.geojobs.service.geoserver.GeoServerConfiguration;
import app.bpartners.geojobs.validator.FeatureTypeChecker;
import java.util.List;
import org.junit.jupiter.api.Test;

class DetectionCreationMapperTest {

  DetectableObjectTypeMapper detectableObjectTypeMapper = new DetectableObjectTypeMapper();
  FeatureTypeChecker featureTypeChecker = new FeatureTypeChecker();
  GeometryConverter geometryConverterMock = mock();
  BuildingFinder buildingFinderMock = mock();

  DetectionCreationMapper subject =
      new DetectionCreationMapper(
          detectableObjectTypeMapper,
          featureTypeChecker,
          mock(CommunityAuthorizationRepository.class),
          mock(AreaPictureApi.class),
          mock(GeoServerConfiguration.class),
          geometryConverterMock,
          buildingFinderMock);

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
