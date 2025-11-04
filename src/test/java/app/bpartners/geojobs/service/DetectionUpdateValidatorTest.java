package app.bpartners.geojobs.service;

import static app.bpartners.geojobs.endpoint.rest.model.ModelName.*;
import static org.junit.jupiter.api.Assertions.*;

import app.bpartners.geojobs.endpoint.rest.controller.mapper.FeatureMapper;
import app.bpartners.geojobs.endpoint.rest.model.*;
import app.bpartners.geojobs.repository.model.detection.Detection;
import app.bpartners.geojobs.utils.FeatureCreator;
import java.util.List;
import lombok.NonNull;
import org.junit.jupiter.api.Test;

class DetectionUpdateValidatorTest {
  DetectionUpdateValidator subject = new DetectionUpdateValidator();
  FeatureCreator featureCreator = new FeatureCreator();

  @Test
  void do_nothing_with_same_attributes() {
    List<Feature> geoJsonZone = featureCreator.defaultFeatures();
    List<app.bpartners.geojobs.repository.model.Feature> domainFeoJsonZone =
        geoJsonZone.stream().map(FeatureMapper::toDomainFeature).toList();
    GeoServerProperties geoServerProperties = new GeoServerProperties();

    assertDoesNotThrow(
        () ->
            subject.accept(
                new Detection(),
                new CreateDetection()
                    .geoServerProperties(geoServerProperties)
                    .geoJsonZone(geoJsonZone)));
    assertDoesNotThrow(
        () ->
            subject.accept(
                Detection.builder()
                    .providedGeoJsonZone(domainFeoJsonZone)
                    .geoServerProperties(geoServerProperties)
                    .detectableObjectModel(new DetectableObjectModel().modelName(LOM))
                    .build(),
                new CreateDetection()
                    .geoServerProperties(geoServerProperties)
                    .detectableObjectModel(new DetectableObjectModel().modelName(LOM))
                    .geoJsonZone(geoJsonZone)));
    assertDoesNotThrow(
        () ->
            subject.accept(
                Detection.builder()
                    .providedGeoJsonZone(domainFeoJsonZone)
                    .geoServerProperties(geoServerProperties)
                    .detectableObjectModel(new DetectableObjectModel().modelName(TOITURE))
                    .build(),
                new CreateDetection()
                    .geoServerProperties(geoServerProperties)
                    .detectableObjectModel(new DetectableObjectModel().modelName(TOITURE))
                    .geoJsonZone(geoJsonZone)));
    assertDoesNotThrow(
        () ->
            subject.accept(
                Detection.builder()
                    .providedGeoJsonZone(domainFeoJsonZone)
                    .geoServerProperties(geoServerProperties)
                    .detectableObjectModel(new DetectableObjectModel().modelName(ZAN))
                    .build(),
                new CreateDetection()
                    .geoServerProperties(geoServerProperties)
                    .detectableObjectModel(new DetectableObjectModel().modelName(ZAN))
                    .geoJsonZone(geoJsonZone)));
  }

  @Test
  void throws_exception_with_updated_attributes() {
    List<Feature> geoJsonZone = featureCreator.defaultFeatures();
    List<app.bpartners.geojobs.repository.model.Feature> domainFeoJsonZone =
        geoJsonZone.stream().map(FeatureMapper::toDomainFeature).toList();
    GeoServerProperties geoServerProperties = new GeoServerProperties();

    var geoServerAndBPLomModelUpdateAttemptException =
        assertThrows(
            IllegalArgumentException.class,
            () ->
                subject.accept(
                    Detection.builder()
                        .providedGeoJsonZone(domainFeoJsonZone)
                        .geoServerProperties(geoServerProperties)
                        .detectableObjectModel(new DetectableObjectModel().modelName(LOM))
                        .build(),
                    new CreateDetection()
                        .geoServerProperties(new GeoServerProperties().geoServerUrl("dummyUrl"))
                        .detectableObjectModel(new DetectableObjectModel().modelName(TOITURE))
                        .geoJsonZone(null)));
    var geoJsonAndBPToitureModelUpdateAttemptException =
        assertThrows(
            IllegalArgumentException.class,
            () ->
                subject.accept(
                    Detection.builder()
                        .providedGeoJsonZone(
                            List.of(new app.bpartners.geojobs.repository.model.Feature()))
                        .geoServerProperties(geoServerProperties)
                        .detectableObjectModel(new DetectableObjectModel().modelName(TOITURE))
                        .build(),
                    new CreateDetection()
                        .geoServerProperties(new GeoServerProperties())
                        .detectableObjectModel(new DetectableObjectModel().modelName(LOM))
                        .geoJsonZone(null)));

    assertEquals(
        expectedGeoServerAndBPLomModelUpdateAttemptException(),
        geoServerAndBPLomModelUpdateAttemptException.getMessage());
    assertEquals(
        expectedGeoJsonAndBPToitureModelUpdateAttemptException(),
        geoJsonAndBPToitureModelUpdateAttemptException.getMessage());
  }

  @NonNull
  private String expectedGeoJsonAndBPToitureModelUpdateAttemptException() {
    return """
Detection.geoJsonZone can not be updated once it has values, otherwise actual value [null] is not equals provided value null. Detection.detectableObjectModel can not be updated once it has values, otherwise actual value class DetectableObjectModel {
    modelName: BP_TOITURE
} is not equals provided value class DetectableObjectModel {
    modelName: BP_LOM
}.\s""";
  }

  @NonNull
  private String expectedGeoServerAndBPLomModelUpdateAttemptException() {
    return """
Detection.geoJsonZone can not be updated once it has values, otherwise actual value [class Feature {
    type: Feature
    geometry: class class app.bpartners.geojobs.endpoint.rest.model.FeatureGeometry {
        instance: class MultiPolygon {
            coordinates: [[[[4.459648282829194, 45.90498891262069], [4.464709510872551, 45.928950368349426], [4.490816965688656, 45.941784543770964], [4.510354299995861, 45.9336971326646], [4.518386257467152, 45.91288834552105], [4.496344031095243, 45.88343820140181], [4.479593950305621, 45.882900828315755], [4.459648282829194, 45.90498891262069]]]]
            type: MultiPolygon
        }
        isNullable: false
        schemaType: oneOf
    }
    properties: {zoom=20, code=69, id=feature1_id, CLUSTER_SIZE=386884, CLUSTER_ID=99520, nom=Rhône}
}] is not equals provided value null. Detection.detectableObjectModel can not be updated once it has values, otherwise actual value class DetectableObjectModel {
    modelName: BP_LOM
} is not equals provided value class DetectableObjectModel {
    modelName: BP_TOITURE
}.\s""";
  }
}
