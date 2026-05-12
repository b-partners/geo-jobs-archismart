package app.bpartners.geojobs.service.event;

import static app.bpartners.geojobs.endpoint.rest.controller.mapper.FeatureMapper.toDomainFeature;
import static app.bpartners.geojobs.endpoint.rest.model.Detection.GeoJsonDelimitationTypeEnum.ROOF;
import static app.bpartners.geojobs.endpoint.rest.model.MultiPolygon.TypeEnum.MULTI_POLYGON;
import static app.bpartners.geojobs.service.geojson.GeoJsonMapper.convertPixelToGeographicalCoordinates;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.FeatureVggRequested;
import app.bpartners.geojobs.endpoint.event.model.FeatureWithDetectionPropertiesRequested;
import app.bpartners.geojobs.endpoint.rest.model.*;
import app.bpartners.geojobs.model.DetectedTile;
import app.bpartners.geojobs.model.geometry.PolygonObjectType;
import app.bpartners.geojobs.repository.DetectionRepository;
import app.bpartners.geojobs.repository.MachineDetectedTileRepository;
import app.bpartners.geojobs.service.FeatureRoofResultPropertiesComputer;
import app.bpartners.geojobs.service.geojson.GeometryConverter;
import app.bpartners.geojobs.service.geojson.GeometryCorrector;
import jakarta.persistence.EntityManager;
import java.util.*;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Geometry;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class FeatureWithDetectionPropertiesRequestedService
    implements Consumer<FeatureWithDetectionPropertiesRequested> {
  private static final int DEFAULT_IMAGE_SIZE = 1024;
  private final EntityManager entityManager;
  private final DetectionRepository detectionRepository;
  private final FeatureRoofResultPropertiesComputer featureRoofResultPropertiesComputer;
  private final GeometryConverter geometryConverter;
  private final MachineDetectedTileRepository machineDetectedTileRepository;
  private final EventProducer eventProducer;
  private final GeometryCorrector geometryCorrector;

  @Override
  public void accept(FeatureWithDetectionPropertiesRequested event) {
    var detectionIdentifier = event.getDetectionIdentifier();
    var feature = event.getFeature();
    entityManager.clear();
    var detection = detectionRepository.findById(detectionIdentifier).orElseThrow();
    var delimitations = detection.getDelimitationOf(feature);
    var savedDetection = apply(detection, feature, delimitations);
    if (savedDetection != null) {
      var optionalProvidedUpdatedFeature =
          savedDetection.getProvidedGeoJsonZone().stream()
              .filter(
                  providedGeoJsonZone -> {
                    var idKey = "id";
                    var featureIdKey = "feature_id";
                    return isPropertyEquals(idKey, providedGeoJsonZone, feature)
                        || isPropertyEquals(featureIdKey, providedGeoJsonZone, feature);
                  })
              .findFirst();
      if (optionalProvidedUpdatedFeature.isPresent()) {
        eventProducer.accept(
            List.of(
                new FeatureVggRequested(
                    detectionIdentifier, optionalProvidedUpdatedFeature.get())));
      } else {
        eventProducer.accept(List.of(new FeatureVggRequested(detectionIdentifier, feature)));
      }
    }
  }

  private boolean isPropertyEquals(
      String featureProperty, Feature providedFeature, Feature actualFeature) {
    return providedFeature.getProperties() != null
        && actualFeature.getProperties() != null
        && providedFeature.getProperties().get(featureProperty) != null
        && actualFeature.getProperties().get(featureProperty) != null
        && providedFeature
            .getProperties()
            .get(featureProperty)
            .equals(actualFeature.getProperties().get(featureProperty));
  }

  public app.bpartners.geojobs.repository.model.detection.Detection apply(
      app.bpartners.geojobs.repository.model.detection.Detection detection,
      Feature currentFeature,
      List<Feature> delimitationsFeature) {
    if (!detection.hasToitureModelName()) {
      log.error("Only BP_TOITURE model is supported to generated VGG from now");
      return null;
    }
    var geoJsonDelimitationType = detection.getGeoJsonDelimitationType();
    var delimitationFeatureWithResultProperties =
        delimitationsFeature.stream()
            .map(
                delimitationFeature -> {
                  var latLonRoofGeometry =
                      getLonLatGeometryIntersectedWithCurrentFeature(
                          geoJsonDelimitationType, delimitationFeature, currentFeature);
                  var detectedObjectPolygonGeometriesUsedForRateComputing =
                      getDetectedObjectPolygonGeometriesUsedForRateComputing(
                          detection.getZdjId(), latLonRoofGeometry);
                  var computedProperties =
                      featureRoofResultPropertiesComputer.apply(
                          delimitationFeature,
                          latLonRoofGeometry,
                          latLonRoofGeometry,
                          detectedObjectPolygonGeometriesUsedForRateComputing);

                  HashMap<String, Object> actualProperties = new HashMap<>();
                  var featureProperties = delimitationFeature.getProperties();
                  if (featureProperties != null) {
                    actualProperties.putAll(featureProperties);
                  }
                  actualProperties.putAll(computedProperties);

                  return toDomainFeature(
                      new Feature()
                          .type(delimitationFeature.getType())
                          .properties(actualProperties)
                          .geometry(delimitationFeature.getGeometry()));
                })
            .toList();

    detection.addFeatureDelimitations(
        toDomainFeature(currentFeature), delimitationFeatureWithResultProperties);

    return detectionRepository.save(detection);
  }

  private List<PolygonObjectType> getDetectedObjectPolygonGeometriesUsedForRateComputing(
      String machineZDJIdentifier, Geometry latLonDelimitationObjectType) {
    var machineDetectedTiles =
        machineDetectedTileRepository.findAllByZdjJobId(machineZDJIdentifier);
    return machineDetectedTiles.stream()
        .map(
            machineDetectedTile ->
                DetectedTile.builder()
                    .tile(machineDetectedTile.getTile())
                    .detectedObjects(machineDetectedTile.getDetectedObjects())
                    .build())
        .map(
            detectedTile -> {
              var tile = detectedTile.getTile();
              var xTile = tile.getCoordinates().getX();
              var yTile = tile.getCoordinates().getY();
              var zoom = tile.getCoordinates().getZ();
              return detectedTile.getDetectedObjects().stream()
                  .map(
                      detectedObject -> {
                        var multiPolygonDetectedObject =
                            getMultiPolygonFromRestFeatureGeometryInstance(
                                detectedObject.getFeature().getGeometry().getActualInstance());
                        var multiPolygonDetectedObjectCoordinates =
                            multiPolygonDetectedObject.getCoordinates();
                        var latLonCoordinates =
                            convertPixelToGeographicalCoordinates(
                                xTile,
                                yTile,
                                zoom,
                                DEFAULT_IMAGE_SIZE,
                                multiPolygonDetectedObjectCoordinates);
                        var detectableObjectType = detectedObject.getDetectableObjectType();
                        var latLonMultiPolygonDetectedObject =
                            geometryConverter.apply(latLonCoordinates);

                        var correctedLatLonDetectedObjectGeometry =
                            geometryCorrector.apply(latLonMultiPolygonDetectedObject);

                        var intersectionBetweenDetectedObjectAndDelimitationObjectType =
                            latLonDelimitationObjectType.intersection(
                                correctedLatLonDetectedObjectGeometry);
                        if (intersectionBetweenDetectedObjectAndDelimitationObjectType.isEmpty()) {
                          return null;
                        }
                        List<PolygonObjectType> polygonObjectTypes = new ArrayList<>();
                        if (intersectionBetweenDetectedObjectAndDelimitationObjectType
                            instanceof org.locationtech.jts.geom.Polygon polygon) {
                          polygonObjectTypes.add(
                              new PolygonObjectType(polygon, detectableObjectType));
                        } else if (intersectionBetweenDetectedObjectAndDelimitationObjectType
                            instanceof org.locationtech.jts.geom.MultiPolygon multiPolygon) {
                          for (int i = 0; i < multiPolygon.getNumGeometries(); i++) {
                            polygonObjectTypes.add(
                                new PolygonObjectType(
                                    (org.locationtech.jts.geom.Polygon)
                                        multiPolygon.getGeometryN(i),
                                    detectableObjectType));
                          }
                        }
                        return polygonObjectTypes;
                      })
                  .filter(Objects::nonNull)
                  .flatMap(List::stream)
                  .toList();
            })
        .flatMap(List::stream)
        .toList();
  }

  private Geometry getLonLatGeometryIntersectedWithCurrentFeature(
      Detection.GeoJsonDelimitationTypeEnum geoJsonDelimitationType,
      Feature currentDelimitationFeature,
      Feature currentFeature) {
    var currentDelimitationFeatureRestMultiPolygon =
        getMultiPolygonFromRestFeatureGeometryInstance(
            currentDelimitationFeature.getGeometry().getActualInstance());
    var currentDelimitationGeometry =
        geometryConverter.apply(currentDelimitationFeatureRestMultiPolygon.getCoordinates());
    if (currentFeature.getGeometry().getActualInstance() instanceof Point) {
      return currentDelimitationGeometry;
    }
    if (ROOF.equals(geoJsonDelimitationType)) {
      var geometryInstance = currentDelimitationFeature.getGeometry().getActualInstance();
      var multiPolygon = getMultiPolygonFromRestFeatureGeometryInstance(geometryInstance);
      return geometryConverter.apply(multiPolygon.getCoordinates());
    }
    var currentFeatureMultiPolygonGeometry =
        geometryConverter.apply(
            getMultiPolygonFromRestFeatureGeometryInstance(
                    currentFeature.getGeometry().getActualInstance())
                .getCoordinates());
    var intersectionBetweenFeatureMultiPolygonAndDelimitationMultiPolygon =
        currentDelimitationGeometry.intersection(currentFeatureMultiPolygonGeometry);
    if (intersectionBetweenFeatureMultiPolygonAndDelimitationMultiPolygon.isEmpty()) {
      throw new IllegalStateException("No intersection between feature and delimitation");
    }
    return intersectionBetweenFeatureMultiPolygonAndDelimitationMultiPolygon;
  }

  private MultiPolygon getMultiPolygonFromRestFeatureGeometryInstance(Object geometryInstance) {
    if (geometryInstance instanceof Polygon polygon) {
      return new MultiPolygon().type(MULTI_POLYGON).coordinates(List.of(polygon.getCoordinates()));
    } else if (geometryInstance instanceof MultiPolygon m) {
      return m;
    } else {
      throw new IllegalArgumentException(
          "Unsupported geometry type " + geometryInstance.getClass());
    }
  }
}
