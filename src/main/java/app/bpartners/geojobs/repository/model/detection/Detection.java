package app.bpartners.geojobs.repository.model.detection;

import static app.bpartners.geojobs.endpoint.rest.controller.mapper.FeatureMapper.toRestFeature;
import static app.bpartners.geojobs.endpoint.rest.model.DelimitationType.PARCEL;
import static app.bpartners.geojobs.endpoint.rest.model.DelimitationType.PARCEL_CONSTRAINED_DELIMITATION;
import static app.bpartners.geojobs.endpoint.rest.model.DetectionStepName.POST_PROCESSING;
import static app.bpartners.geojobs.endpoint.rest.model.ModelName.TOITURE;
import static app.bpartners.geojobs.job.model.Status.HealthStatus.SUCCEEDED;
import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.FINISHED;
import static app.bpartners.geojobs.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;
import static jakarta.persistence.CascadeType.ALL;
import static jakarta.persistence.EnumType.STRING;
import static jakarta.persistence.FetchType.EAGER;
import static java.time.Instant.now;
import static java.util.UUID.randomUUID;
import static org.hibernate.type.SqlTypes.JSON;
import static org.hibernate.type.SqlTypes.NAMED_ENUM;

import app.bpartners.geojobs.endpoint.rest.controller.mapper.FeatureMapper;
import app.bpartners.geojobs.endpoint.rest.model.*;
import app.bpartners.geojobs.endpoint.rest.validator.FeatureTypeChecker;
import app.bpartners.geojobs.model.exception.ApiException;
import app.bpartners.geojobs.repository.model.Feature;
import app.bpartners.geojobs.repository.model.feature.FeatureDelimitationComputing;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.annotations.JdbcTypeCode;

@Slf4j
@Entity
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Builder(toBuilder = true)
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Table(name = "detection")
public class Detection implements Serializable {
  @Id private String id;
  private String endToEndId;

  @Column(name = "geojson_s3_file_key")
  private String geojsonS3FileKey;

  private String shapeFileKey;
  private String excelFileKey;
  private String imageFileKey;
  private String pdfFileKey;
  private String vggFileKey;
  private Integer imageWidth;
  private Integer imageHeight;
  private boolean integrationTest;

  @Column(name = "zdj_id")
  private String zdjId;

  @Column(name = "ztj_id")
  private String ztjId;

  private String zoneName;

  private String emailReceiver;

  @Getter(AccessLevel.NONE)
  private Boolean isSynchronous;

  @Getter(AccessLevel.NONE)
  private Boolean isOutputZipped;

  @Getter(AccessLevel.NONE)
  private Boolean needsImageOutput;

  @JoinColumn(referencedColumnName = "id", name = "community_owner_id")
  private String communityOwnerId;

  // TODO: save as entity
  @JdbcTypeCode(JSON)
  private List<DetectableObjectConfiguration> detectableObjectConfigurations;

  @JdbcTypeCode(JSON)
  private List<DetectableObjectModel> detectableObjectModelList;

  // TODO: save as entity
  @JdbcTypeCode(JSON)
  private GeoServerProperties geoServerProperties;

  @Column(name = "geo_json_zone")
  @JdbcTypeCode(JSON)
  @Getter(AccessLevel.NONE)
  private List<Feature> providedGeoJsonZone;

  @Getter(AccessLevel.NONE)
  @Column(name = "to_notify")
  private Boolean toNotify;

  @JdbcTypeCode(JSON)
  @Getter(AccessLevel.NONE)
  private List<Feature> multiPolygonGeoJsonZone;

  @JdbcTypeCode(JSON)
  @Getter(AccessLevel.NONE)
  private Feature polygonGeoJsonZone;

  @JdbcTypeCode(JSON)
  @Getter(AccessLevel.NONE)
  private List<Feature> splitPolygonGeoJsonZone;

  @JdbcTypeCode(JSON)
  @Getter(AccessLevel.NONE)
  private HashMap<String, Feature> pointDelimitation;

  @OneToMany(cascade = ALL, fetch = EAGER, orphanRemoval = true, mappedBy = "detection")
  private List<FeatureDelimitationComputing> featureDelimitationComputingList;

  @JdbcTypeCode(JSON)
  @Getter(AccessLevel.NONE)
  private List<FeatureWithDelimitation> featureWithDelimitations;

  @JdbcTypeCode(JSON)
  private List<FeatureWithDelimitation> parcelDelimitations;

  @JdbcTypeCode(JSON)
  private List<String> convertedAddresses;

  @JdbcTypeCode(JSON)
  private List<List<BigDecimal>> polygonRoofDelimitation;

  @Enumerated(STRING)
  @JdbcTypeCode(NAMED_ENUM)
  private DelimitationType geoJsonDelimitationType;

  @OneToMany(fetch = EAGER, cascade = ALL)
  @JoinColumn(name = "detection_id")
  private List<DetectionStep> detectionSteps = new ArrayList<>();

  @Column(nullable = true, updatable = false)
  private Instant creationDatetime;

  @JdbcTypeCode(JSON)
  @Getter(AccessLevel.NONE)
  private DetectableObjectModel detectableObjectModel;

  @OneToMany(mappedBy = "detection", fetch = EAGER, cascade = ALL, orphanRemoval = true)
  private List<DetectionFeature> detectionFeatures;

  @Column(name = "roof_properties_computation_creation_datetime")
  private Instant roofPropertiesComputationCreationDatetime;

  public List<FeatureWithDelimitation> getFeatureWithDelimitations() {
    if (featureDelimitationComputingList == null || featureDelimitationComputingList.isEmpty()) {
      return featureWithDelimitations;
    }
    Map<String, FeatureDelimitationComputing> latestComputingByFeature =
        featureDelimitationComputingList.stream()
            .collect(
                Collectors.toMap(
                    FeatureDelimitationComputing::getFeaturePropertiesIdentifier,
                    Function.identity(),
                    BinaryOperator.maxBy(
                        Comparator.comparing(FeatureDelimitationComputing::getCreationDatetime))));
    return featureWithDelimitations.stream()
        .map(
            original -> {
              String featureId =
                  original.getRestFeature().getProperties().get("feature_id").toString();
              FeatureDelimitationComputing computing = latestComputingByFeature.get(featureId);
              if (computing == null) {
                return original;
              }
              return new FeatureWithDelimitation(
                  original.feature(), computing.getFeatureWithDelimitation().delimitations());
            })
        .toList();
  }

  public boolean hasParcelDelimitationType() {
    return getGeoJsonDelimitationType() != null
        && (PARCEL.equals(getGeoJsonDelimitationType())
            || PARCEL_CONSTRAINED_DELIMITATION.equals(getGeoJsonDelimitationType()));
  }

  public DetectableObjectModel getDetectableObjectModel() {
    if (detectableObjectModelList != null && !detectableObjectModelList.isEmpty()) {
      if (detectableObjectModelList.size() > 1) {
        log.info(
            "More than one detectable object model found for detection {}. Using the first one: {}",
            id,
            detectableObjectModelList.getFirst().getModelName());
      }
      return detectableObjectModelList.getFirst();
    }
    if (detectableObjectModel != null) return detectableObjectModel;
    return null;
  }

  @PrePersist
  protected void onCreate() {
    this.creationDatetime = now().truncatedTo(ChronoUnit.MICROS);
    if (detectableObjectModelList == null) {
      detectableObjectModelList = new ArrayList<>();
    }
  }

  public void addStep(DetectionStep step) {
    if (detectionSteps == null) {
      detectionSteps = new ArrayList<>();
    }
    detectionSteps.add(step);
  }

  public DetectionStep getStep() {
    return detectionSteps == null
        ? null
        : detectionSteps.stream()
            .max(Comparator.comparing(DetectionStep::getCreationDatetime))
            .orElse(null);
  }

  public boolean isOnStepPostProcessingSucceeded() {
    var detectionStep = getStep();
    if (detectionStep == null) {
      return false;
    }
    return POST_PROCESSING.equals(detectionStep.getName())
        && FINISHED.equals(detectionStep.getProgression())
        && SUCCEEDED.equals(detectionStep.getHealth());
  }

  public boolean isOutputZipped() {
    return isOutputZipped != null && isOutputZipped;
  }

  public boolean needsImageOutput() {
    return needsImageOutput != null && needsImageOutput;
  }

  public boolean isSynchronous() {
    return isSynchronous != null && isSynchronous;
  }

  public boolean isToNotify() {
    return toNotify != null && toNotify;
  }

  public List<app.bpartners.geojobs.endpoint.rest.model.Feature> getSplitPolygonGeoJsonZone() {
    return splitPolygonGeoJsonZone == null
        ? null
        : splitPolygonGeoJsonZone.stream().map(FeatureMapper::toRestFeature).toList();
  }

  public List<app.bpartners.geojobs.endpoint.rest.model.Feature> getProvidedGeoJsonZone() {
    if (detectionFeatures != null && !detectionFeatures.isEmpty()) {
      return getDomainProvidedFeaturesThroughDetectionFeature().stream()
          .map(FeatureMapper::toRestFeature)
          .toList();
    }
    return providedGeoJsonZone == null
        ? null
        : providedGeoJsonZone.stream().map(FeatureMapper::toRestFeature).toList();
  }

  public List<Feature> getDomainProvidedGeoJsonZone() {
    if (detectionFeatures != null) {
      return getDomainProvidedFeaturesThroughDetectionFeature();
    }
    return providedGeoJsonZone == null ? List.of() : providedGeoJsonZone;
  }

  private List<Feature> getDomainProvidedFeaturesThroughDetectionFeature() {
    return detectionFeatures.stream()
        .collect(
            Collectors.toMap(
                DetectionFeature::getIdFeature,
                Function.identity(),
                (a1, a2) -> a1.getCreationDatetime().isAfter(a2.getCreationDatetime()) ? a1 : a2))
        .values()
        .stream()
        .map(DetectionFeature::getFeature)
        .toList();
  }

  public List<app.bpartners.geojobs.endpoint.rest.model.Feature> getMultiPolygonGeoJsonZone() {
    return multiPolygonGeoJsonZone == null
        ? null
        : multiPolygonGeoJsonZone.stream().map(FeatureMapper::toRestFeature).toList();
  }

  public app.bpartners.geojobs.endpoint.rest.model.Feature getPolygonGeoJsonZone() {
    return polygonGeoJsonZone == null ? null : toRestFeature(polygonGeoJsonZone);
  }

  public HashMap<
          app.bpartners.geojobs.endpoint.rest.model.Feature,
          app.bpartners.geojobs.endpoint.rest.model.Feature>
      getPointDelimitation() {
    return pointDelimitation == null
        ? new HashMap<>()
        : pointDelimitation.entrySet().stream()
            .collect(
                Collectors.toMap(
                    entry -> {
                      try {
                        return toRestFeature(
                            new ObjectMapper()
                                .findAndRegisterModules()
                                .readValue(entry.getKey(), Feature.class));
                      } catch (JsonProcessingException e) {
                        throw new ApiException(SERVER_EXCEPTION, e);
                      }
                    },
                    entry -> toRestFeature(entry.getValue()),
                    (v1, v2) -> v1,
                    HashMap::new));
  }

  public boolean hasOnlyPointsGeoJson() {
    return getMultiPolygonGeoJsonZone() != null
        && new FeatureTypeChecker().apply(getProvidedGeoJsonZone(), Point.class);
  }

  public boolean hasToitureModelName() {
    return (detectableObjectModelList != null
            && !detectableObjectModelList.isEmpty()
            && detectableObjectModelList.stream()
                .anyMatch(model -> TOITURE.equals(model.getModelName())))
        || (detectableObjectModel != null
            && detectableObjectModel.getModelName() != null
            && TOITURE.equals(detectableObjectModel.getModelName()));
  }

  public boolean isSucceeded() {
    return getGeojsonS3FileKey() != null;
  }

  public boolean isStillOnConfiguringStep() {
    return getMultiPolygonGeoJsonZone() == null
        || getMultiPolygonGeoJsonZone().isEmpty()
        || getGeoServerProperties() == null;
  }

  public boolean isStillOnTilingStep() {
    return getZdjId() == null;
  }

  public boolean isTilingPending() {
    return getZtjId() == null && !isStillOnConfiguringStep();
  }

  public boolean isMachineDetectionStepProcessing(ZoneDetectionJob zoneDetectionJob) {
    return !zoneDetectionJob.isFinished();
  }

  private boolean isMachineDetectionFinished(ZoneDetectionJob zoneDetectionJob) {
    return zoneDetectionJob.isFinished();
  }

  public boolean isHumanDetectionStepProcessing(ZoneDetectionJob zoneDetectionJob) {
    return isMachineDetectionFinished(zoneDetectionJob) && geojsonS3FileKey == null;
  }

  public List<DetectionFeature> addFeatures(
      List<Feature> features, DetectionFeatureType detectionFeatureType) {
    if (detectionFeatures == null) {
      detectionFeatures = new ArrayList<>();
    }
    detectionFeatures.addAll(
        features.stream()
            .map(
                feature ->
                    DetectionFeature.builder()
                        .id(randomUUID().toString())
                        .detection(this)
                        .idFeature(feature.getId())
                        .detectionFeatureType(detectionFeatureType)
                        .feature(feature)
                        .build())
            .toList());

    return detectionFeatures;
  }

  public List<FeatureDelimitationComputing> addFeatureDelimitations(
      Feature currentFeature, List<Feature> delimitations) {
    if (featureDelimitationComputingList == null) {
      featureDelimitationComputingList = new ArrayList<>();
    }

    featureDelimitationComputingList.add(
        FeatureDelimitationComputing.builder()
            .id(randomUUID().toString())
            .detection(this)
            .featurePropertiesIdentifier(currentFeature.getId())
            .featureWithDelimitation(new FeatureWithDelimitation(currentFeature, delimitations))
            .creationDatetime(now())
            .build());

    return featureDelimitationComputingList;
  }

  public List<app.bpartners.geojobs.endpoint.rest.model.Feature> getTilingRestFeatures() {
    if (getFeatureWithDelimitations() != null && !getFeatureWithDelimitations().isEmpty()) {
      return getFeatureWithDelimitations().stream()
          .map(
              featureWithDelimitation -> {
                var featureProperties = featureWithDelimitation.feature().getProperties();
                return featureWithDelimitation.getRestDelimitations().stream()
                    .map(
                        delimitationFeature -> {
                          return copyFeatureProperties(delimitationFeature, featureProperties);
                        })
                    .toList();
              })
          .flatMap(List::stream)
          .toList();
    }
    return getProvidedGeoJsonZone();
  }

  public List<app.bpartners.geojobs.endpoint.rest.model.Feature> getDelimitationOf(
      app.bpartners.geojobs.endpoint.rest.model.Feature feature) {
    return getFeatureWithDelimitations().stream()
        .filter(
            featureWithDelimitation ->
                Objects.equals(
                    featureWithDelimitation.getRestFeature().getGeometry(), feature.getGeometry()))
        .map(
            featureWithDelimitation -> {
              var featureProperties = featureWithDelimitation.feature().getProperties();
              return featureWithDelimitation.getRestDelimitations().stream()
                  .map(
                      delimitationFeature ->
                          copyFeatureProperties(delimitationFeature, featureProperties))
                  .toList();
            })
        .flatMap(List::stream)
        .toList();
  }

  private app.bpartners.geojobs.endpoint.rest.model.Feature copyFeatureProperties(
      app.bpartners.geojobs.endpoint.rest.model.Feature feature,
      HashMap<String, Object> featureProperties) {
    var actualProperties = new HashMap<String, Object>();
    var delimitationProperties = feature.getProperties();
    if (featureProperties != null) {
      actualProperties.putAll(featureProperties);
    }
    if (delimitationProperties != null) {
      actualProperties.putAll(delimitationProperties);
    }
    return feature.properties(actualProperties);
  }
}
