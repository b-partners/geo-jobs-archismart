package app.bpartners.geojobs.service;

import static app.bpartners.geojobs.service.event.DetectionRoofSlopeAndHeightRequestedService.*;

import app.bpartners.geojobs.endpoint.rest.model.Feature;
import app.bpartners.geojobs.model.geometry.PolygonObjectType;
import app.bpartners.geojobs.model.geometry.area.rate.AreaRateComputerFacade;
import app.bpartners.geojobs.service.area.mutation.MutationComputer;
import app.bpartners.geojobs.service.area.mutation.model.AreaPictureHistoryResponse;
import app.bpartners.geojobs.service.area.mutation.model.MutationContext;
import app.bpartners.geojobs.service.area.mutation.model.MutationType;
import app.bpartners.geojobs.service.area.toiture.model.CoveringType;
import app.bpartners.geojobs.service.area.toiture.model.FireRiskLevel;
import app.bpartners.geojobs.service.area.toiture.model.RoofAssessmentResult;
import app.bpartners.geojobs.service.area.toiture.service.RoofAssessmentFacade;
import app.bpartners.geojobs.service.area.toiture.service.RoofVegetationContextEvaluator;
import app.bpartners.geojobs.service.event.DetectionRoofPropertiesRequestedService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;
import org.locationtech.jts.geom.Geometry;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class FeatureRoofResultPropertiesComputer {
  private final GeometrySquareMeterArea geometrySquareMeterArea;
  private final PolygonObjectTypeConverter polygonObjectTypeConverter;
  private final RoofAssessmentFacade roofAssessmentFacade;
  private final ObjectMapper objectMapper;
  private final MutationComputer mutationComputer;

  public HashMap<String, Object> apply(
      Feature feature,
      Geometry geometryUsedForAreaComputing,
      Geometry roofGeometryUsedForRateComputing,
      Collection<PolygonObjectType> detectedObjectPolygonGeometriesUsedForRateComputing,
      @Nullable MutationContext mutationContext) {

    var actualProperties = new HashMap<String, Object>();

    if (feature.getProperties() != null) {
      actualProperties.putAll(feature.getProperties());
    }

    var roofAreaInSquareMeters = geometrySquareMeterArea.apply(geometryUsedForAreaComputing);
    actualProperties.put("roof_area_in_m2", roofAreaInSquareMeters);

    var addresses = retrieveAddressesProperty(feature);
    actualProperties.put("addresses", addresses);

    var rateComputer =
        new AreaRateComputerFacade(
            roofGeometryUsedForRateComputing, detectedObjectPolygonGeometriesUsedForRateComputing);
    var vegetationEvaluator =
        new RoofVegetationContextEvaluator(
            roofGeometryUsedForRateComputing,
            roofAreaInSquareMeters,
            polygonObjectTypeConverter.convertFrom(
                detectedObjectPolygonGeometriesUsedForRateComputing),
            retrieveRoofSlopeInDegrees(feature),
            retrieveCoveringType(feature),
            false /* hasDrainageSystem not yet available from detection */);
    var usureRate = rateComputer.getUsureAreaRate();
    var humiditeRate = rateComputer.getHumidityAreaRate();
    var moisissureRate = rateComputer.getMoisissureAreaRate();
    var roofAssessment = computeRoofAssessment(vegetationEvaluator);
    var vegetationFeu = toVegetationFeu(roofAssessment, vegetationEvaluator.hasVegetationData());
    var globalRateValue = rateComputer.getGlobalRate(vegetationFeu);
    var globalRateType = rateComputer.getRate(vegetationFeu);

    actualProperties.put("usure_rate", usureRate);
    actualProperties.put("humidite_rate", humiditeRate);
    actualProperties.put("moisissure_rate", moisissureRate);
    actualProperties.put("global_rate_value", globalRateValue);
    actualProperties.put("global_rate_type", globalRateType);
    if (roofAssessment != null) {
      actualProperties.put("vegetation_index", roofAssessment.vegetationIndex());
      actualProperties.put("fire_risk", roofAssessment.fireRiskLevel());
      actualProperties.put("maintenance_vegetation", roofAssessment.maintenancePriority());
    }

    var detectedRoofCovering = retrieveCoveringProperties(feature);
    if (detectedRoofCovering != null) {
      actualProperties.put(
          "revetement_1",
          detectedRoofCovering.primary() == null ? null : detectedRoofCovering.primary().name());
      actualProperties.put(
          "revetement_2",
          detectedRoofCovering.secondary() == null
              ? null
              : detectedRoofCovering.secondary().name());
    }

    actualProperties.put(
        "mutation",
        mutationContext == null ? MutationType.UNKNOWN : computeMutation(mutationContext));
    if (mutationContext != null) {
      var recentImage = mutationContext.mostRecent();
      var olderImage = mutationContext.older();
      actualProperties.put("mutation_recent_image_url", presignedUrlValue(recentImage));
      actualProperties.put("mutation_recent_image_date", recentImage.year());
      actualProperties.put("mutation_older_image_url", presignedUrlValue(olderImage));
      actualProperties.put("mutation_older_image_date", olderImage.year());
    }

    return actualProperties;
  }

  @Nullable
  private static String presignedUrlValue(AreaPictureHistoryResponse.DatedImage image) {
    var presignedUrl = image.imagePresignedUrl();
    return presignedUrl == null ? null : presignedUrl.value();
  }

  private MutationType computeMutation(MutationContext mutationContext) {
    try {
      return mutationComputer.apply(mutationContext);
    } catch (RuntimeException e) {
      // Downloading/resizing images or calling the mutation API can fail independently of the
      // rest of the roof properties already computed above; don't lose those over it. UNKNOWN is
      // a real domain state here, distinct from MutationType.NONE ("no mutation detected") - we
      // just couldn't determine one.
      log.warn("Could not compute mutation: {}", e.getMessage());
      return MutationType.UNKNOWN;
    }
  }

  @Nullable
  private RoofAssessmentResult computeRoofAssessment(
      RoofVegetationContextEvaluator vegetationEvaluator) {
    try {
      return roofAssessmentFacade.computeAssessment(vegetationEvaluator);
    } catch (RuntimeException e) {
      // Vegetation/fire-risk module not yet reliable or unavailable: fall back to no derived
      // value instead of failing the whole rate computation.
      return null;
    }
  }

  @Nullable
  private static Boolean toVegetationFeu(
      @Nullable RoofAssessmentResult roofAssessment, boolean hasVegetationData) {
    // Vegetation detection isn't requested for every job (e.g. the TOITURE model alone doesn't
    // request it), so absence of vegetation data is not proof of "no risk" - only assert a value
    // when real vegetation was actually detected near the roof, otherwise stay null.
    if (roofAssessment == null || roofAssessment.fireRiskLevel() == null || !hasVegetationData) {
      return null;
    }
    return roofAssessment.fireRiskLevel() != FireRiskLevel.NULL;
  }

  private Double retrieveRoofSlopeInDegrees(Feature feature) {
    if (feature.getProperties() == null
        || feature.getProperties().get(ROOF_SLOPE_PROPERTY_NAME) == null) {
      return null;
    }
    return ((Number) feature.getProperties().get(ROOF_SLOPE_PROPERTY_NAME)).doubleValue();
  }

  private CoveringType retrieveCoveringType(Feature feature) {
    var detectedRoofCovering = retrieveCoveringProperties(feature);
    if (detectedRoofCovering == null || detectedRoofCovering.primary() == null) {
      return null;
    }
    return switch (detectedRoofCovering.primary()) {
      case ROOF_ASPHALTE_BITUME -> CoveringType.HIGH_COMBUSTIBILITY;
      default -> CoveringType.LOW_COMBUSTIBILITY;
    };
  }

  private DetectionRoofPropertiesRequestedService.DetectedRoofCovering retrieveCoveringProperties(
      Feature feature) {
    if (feature.getProperties() == null
        || (feature.getProperties().isEmpty() || feature.getProperties().get("covering") == null)) {
      return null;
    }
    try {
      return objectMapper.readValue(
          feature.getProperties().get("covering").toString(),
          DetectionRoofPropertiesRequestedService.DetectedRoofCovering.class);
    } catch (JsonProcessingException e) {
      return null;
    }
  }

  private List<String> retrieveAddressesProperty(Feature feature) {
    if (feature.getProperties() == null
        || (feature.getProperties().isEmpty()
            || feature.getProperties().get("addresses") == null)) {
      return null;
    }
    try {
      return objectMapper.readValue(
          feature.getProperties().get("addresses").toString(), new TypeReference<>() {});
    } catch (JsonProcessingException e) {
      return null;
    }
  }
}
