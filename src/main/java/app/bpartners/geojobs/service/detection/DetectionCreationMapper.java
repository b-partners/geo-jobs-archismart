package app.bpartners.geojobs.service.detection;

import static app.bpartners.geojobs.endpoint.rest.controller.mapper.FeatureMapper.toDomainFeature;
import static app.bpartners.geojobs.endpoint.rest.model.Feature.TypeEnum.FEATURE;
import static app.bpartners.geojobs.endpoint.rest.model.GeoJsonOutput.ZIP;
import static app.bpartners.geojobs.endpoint.rest.model.ModelName.TOITURE;
import static app.bpartners.geojobs.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;
import static app.bpartners.geojobs.repository.model.detection.DetectionFeatureType.PROVIDED_FEATURE;
import static java.util.UUID.randomUUID;

import app.bpartners.geojobs.endpoint.rest.controller.mapper.DetectableObjectTypeMapper;
import app.bpartners.geojobs.endpoint.rest.controller.mapper.FeatureMapper;
import app.bpartners.geojobs.endpoint.rest.model.*;
import app.bpartners.geojobs.endpoint.rest.validator.FeatureTypeChecker;
import app.bpartners.geojobs.model.exception.ApiException;
import app.bpartners.geojobs.repository.CommunityAuthorizationRepository;
import app.bpartners.geojobs.repository.model.Feature;
import app.bpartners.geojobs.repository.model.community.CommunityAuthorization;
import app.bpartners.geojobs.repository.model.detection.DetectableObjectConfiguration;
import app.bpartners.geojobs.repository.model.detection.Detection;
import app.bpartners.geojobs.service.BuildingFinder;
import app.bpartners.geojobs.service.dashboard.AreaPictureApi;
import app.bpartners.geojobs.service.geojson.GeometryConverter;
import app.bpartners.geojobs.service.geoserver.GeoServerConfiguration;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import javax.annotation.Nullable;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class DetectionCreationMapper {
  private final DetectableObjectTypeMapper detectableObjectTypeMapper;
  private final FeatureTypeChecker featureTypeChecker;
  private final CommunityAuthorizationRepository communityAuthRepository;
  private final AreaPictureApi areaPictureApi;
  private final GeoServerConfiguration geoServerConfiguration;
  private final GeometryConverter geometryConverter;
  private final BuildingFinder buildingFinder;

  public Detection apply(
      CreateDetection createDetection,
      String detectionE2Id,
      @Nullable String communityOwnerId,
      boolean isSynchronous) {
    var detectableObjectModel = createDetection.getDetectableObjectModel();
    var detectableObjectModelList = createDetection.getDetectableObjectModelList();
    var detectionId = randomUUID().toString();
    var detectableObjectConfigurations =
        getDetectableObjectConfigurations(detectionId, createDetection);
    var providedGeoJson = createDetection.getGeoJsonZone();
    var domainProvidedGeoJsonZone = getActualProvidedGeoJson(providedGeoJson);
    List<ModelName> modelNames =
        detectableObjectModel != null
            ? List.of(detectableObjectModel.getModelName())
            : (detectableObjectModelList != null
                ? detectableObjectModelList.stream()
                    .map(DetectableObjectModel::getModelName)
                    .toList()
                : List.of());
    var polygonGeoJsonZoneToBeProcessed =
        extractDetectionPolygonGeoJson(providedGeoJson, modelNames);
    var finalGeoServerProperties =
        extractGeoServerProperties(
            createDetection.getGeoServerProperties(),
            communityOwnerId,
            providedGeoJson,
            domainProvidedGeoJsonZone);
    var detection =
        Detection.builder()
            .id(detectionId)
            .endToEndId(detectionE2Id)
            .emailReceiver(createDetection.getEmailReceiver())
            .zoneName(createDetection.getZoneName())
            .isSynchronous(isSynchronous)
            .communityOwnerId(communityOwnerId)
            .detectableObjectConfigurations(detectableObjectConfigurations)
            .geoServerProperties(finalGeoServerProperties)
            .providedGeoJsonZone(domainProvidedGeoJsonZone)
            .multiPolygonGeoJsonZone(domainProvidedGeoJsonZone)
            .polygonGeoJsonZone(polygonGeoJsonZoneToBeProcessed)
            .detectableObjectModel(detectableObjectModel)
            .detectableObjectModelList(detectableObjectModelList)
            .toNotify(Boolean.TRUE.equals(createDetection.getToNotify()))
            .isOutputZipped(
                createDetection.getGeoJsonOutput() == null
                    || (createDetection.getGeoJsonOutput() != null
                        && ZIP.equals(createDetection.getGeoJsonOutput())))
            .needsImageOutput(
                createDetection.getNeedsImageOutput() != null
                    && createDetection.getNeedsImageOutput())
            .geoJsonDelimitationType(createDetection.getGeoJsonDelimitationType())
            .build();
    detection.addFeatures(domainProvidedGeoJsonZone, PROVIDED_FEATURE);
    return detection;
  }

  private List<DetectableObjectConfiguration> getDetectableObjectConfigurations(
      String detectionId, CreateDetection createDetection) {
    if (createDetection.getDetectableObjectModelList() == null
        || createDetection.getDetectableObjectModelList().isEmpty()) {

      if (createDetection.getDetectableObjectModel() == null) {
        throw new IllegalArgumentException(
            "No detectable object model provided for detectionId: " + detectionId);
      }

      return detectableObjectTypeMapper.mapDefaultConfigurationsFromModel(
          detectionId, createDetection.getDetectableObjectModel().getModelName());
    }

    return createDetection.getDetectableObjectModelList().stream()
        .flatMap(
            model ->
                detectableObjectTypeMapper
                    .mapDefaultConfigurationsFromModel(detectionId, model.getModelName())
                    .stream())
        .toList();
  }

  private List<Feature> getActualProvidedGeoJson(
      List<app.bpartners.geojobs.endpoint.rest.model.Feature> restProvidedGeoJson) {
    if (restProvidedGeoJson == null) {
      return List.of();
    }
    return restProvidedGeoJson.stream()
        .peek(
            getOrSetFeatureIdentifier(
                app.bpartners.geojobs.endpoint.rest.model.Feature::getProperties,
                app.bpartners.geojobs.endpoint.rest.model.Feature::setProperties))
        .map(FeatureMapper::toDomainFeature)
        .toList();
  }

  public static <T> Consumer<T> getOrSetFeatureIdentifier(
      Function<T, Map<String, Object>> getter, BiConsumer<T, Map<String, Object>> setter) {
    return feature -> {
      Map<String, Object> props = getter.apply(feature);

      if (props == null) {
        props = new HashMap<>();
        setter.accept(feature, props);
      }

      props.computeIfAbsent("feature_id", k -> randomUUID().toString());
    };
  }

  private GeoServerProperties extractGeoServerProperties(
      GeoServerProperties geoServerProperties,
      String communityOwnerId,
      List<app.bpartners.geojobs.endpoint.rest.model.Feature> geoJsonZone,
      List<app.bpartners.geojobs.repository.model.Feature> multiPolygonGeoJsonZone) {
    var finalGeoServerProperties = geoServerProperties;
    if (geoJsonZone != null
        && !multiPolygonGeoJsonZone.isEmpty()
        && (geoServerProperties == null
            || geoServerProperties.getGeoServerParameter() == null
            || geoServerProperties.getGeoServerParameter().getLayers() == null)) {
      var firstPoint = retrieveFirstPoint(geoJsonZone);
      List<HashMap<String, String>> layers = retrieveLayers(firstPoint, communityOwnerId);
      String layer = layers.getFirst().get("name");
      int precisionLevelInCm = Integer.parseInt(layers.getFirst().get("precisionLevelInCm"));

      if (precisionLevelInCm != 5) {
        throw new ApiException(
            SERVER_EXCEPTION,
            String.format(
                "Cannot process detection : image resolution must be 5 cm. Current layer = %s"
                    + " (precisionLevelInCm = %d)",
                layer, precisionLevelInCm));
      }

      // TODO: save other layers to be used in failure case
      finalGeoServerProperties =
          geoServerConfiguration.defaultGeoServerProperties(layer, precisionLevelInCm);
    }
    return finalGeoServerProperties;
  }

  private app.bpartners.geojobs.repository.model.Feature extractDetectionPolygonGeoJson(
      List<app.bpartners.geojobs.endpoint.rest.model.Feature> providedGeoJsonZone,
      List<ModelName> modelNames) {
    if (modelNames.contains(TOITURE)
        && providedGeoJsonZone.size() == 1
        && providedGeoJsonZone.getFirst().getGeometry() != null
        && providedGeoJsonZone.getFirst().getGeometry().getActualInstance()
            instanceof Point point) {
      var nearestRoofMultiPolygonGeometry = buildingFinder.getBuildingMultiPolygon(point);
      var nearestRoofMultiPolygonCoordinates =
          geometryConverter.multiPolygonToNestedList(nearestRoofMultiPolygonGeometry);
      return toDomainFeature(
          new app.bpartners.geojobs.endpoint.rest.model.Feature()
              .type(FEATURE)
              .properties(providedGeoJsonZone.getFirst().getProperties())
              .geometry(
                  new FeatureGeometry(
                      new Polygon().coordinates(nearestRoofMultiPolygonCoordinates.getFirst()))));
    }
    var featurePolygonFromMultiPolygon =
        retrieveFeaturePolygonFromMultiPolygon(providedGeoJsonZone);
    if (featurePolygonFromMultiPolygon != null) return featurePolygonFromMultiPolygon;
    if (providedGeoJsonZone.size() != 1) {
      return null;
    }
    return toDomainFeature(providedGeoJsonZone.getFirst());
  }

  private app.bpartners.geojobs.repository.model.Feature retrieveFeaturePolygonFromMultiPolygon(
      List<app.bpartners.geojobs.endpoint.rest.model.Feature> providedGeoJsonZone) {
    if (providedGeoJsonZone.size() == 1
        && featureTypeChecker.apply(providedGeoJsonZone, MultiPolygon.class)
        && providedGeoJsonZone.getFirst().getGeometry().getMultiPolygon().getCoordinates().size()
            == 1
        && providedGeoJsonZone
                .getFirst()
                .getGeometry()
                .getMultiPolygon()
                .getCoordinates()
                .getFirst()
                .size()
            == 1
        && providedGeoJsonZone
                .getFirst()
                .getGeometry()
                .getMultiPolygon()
                .getCoordinates()
                .getFirst()
                .getFirst()
                .size()
            >= 4) {
      return toDomainFeature(
          new app.bpartners.geojobs.endpoint.rest.model.Feature()
              .type(FEATURE)
              .properties(providedGeoJsonZone.getFirst().getProperties())
              .geometry(
                  new FeatureGeometry(
                      new Polygon()
                          .coordinates(
                              providedGeoJsonZone
                                  .getFirst()
                                  .getGeometry()
                                  .getMultiPolygon()
                                  .getCoordinates()
                                  .getFirst()))));
    }
    return null;
  }

  private List<app.bpartners.geojobs.repository.model.Feature> extractDetectionMultiPolygonGeoJson(
      List<app.bpartners.geojobs.endpoint.rest.model.Feature> geoJsonZone,
      List<app.bpartners.geojobs.repository.model.Feature> providedGeoJsonZone) {
    if (providedGeoJsonZone.isEmpty() || geoJsonZone == null) {
      return providedGeoJsonZone;
    }
    return geoJsonZone.stream().map(FeatureMapper::toDomainFeature).toList();
  }

  private List<BigDecimal> retrieveFirstPoint(
      List<app.bpartners.geojobs.endpoint.rest.model.Feature> geoJsonZone) {
    var firstFeature = geoJsonZone.getFirst();
    var firstInstance = firstFeature.getGeometry().getActualInstance();
    if (firstInstance instanceof MultiPolygon multiPolygon) {
      return multiPolygon.getCoordinates().getFirst().getFirst().getFirst();
    } else if (firstInstance instanceof Polygon polygon) {
      return polygon.getCoordinates().getFirst().getFirst();
    } else if (firstInstance instanceof Point point) {
      return point.getCoordinates();
    }
    throw new IllegalArgumentException("Unknown feature type: " + firstFeature);
  }

  private List<HashMap<String, String>> retrieveLayers(
      List<BigDecimal> firstPoint, String communityOwnerId) {
    var longitude = firstPoint.get(0).doubleValue();
    var latitude = firstPoint.get(1).doubleValue();

    var e2ApiKey =
        communityAuthRepository
            .findById(communityOwnerId)
            .map(CommunityAuthorization::getDashboardApiKey)
            .orElseThrow();

    var areaMapLayers = areaPictureApi.getAreaPictureMapLayers(longitude, latitude, e2ApiKey);

    return areaMapLayers.stream()
        .map(
            layer -> {
              HashMap<String, String> map = new HashMap<>();
              map.put("name", layer.name());
              map.put("precisionLevelInCm", String.valueOf(layer.precisionLevelInCm()));
              return map;
            })
        .toList();
  }
}
