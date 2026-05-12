package app.bpartners.geojobs.service;

import static app.bpartners.geojobs.service.event.DetectionRoofSlopeAndHeightRequestedService.*;

import app.bpartners.geojobs.endpoint.rest.model.Feature;
import app.bpartners.geojobs.model.geometry.PolygonObjectType;
import app.bpartners.geojobs.model.geometry.area.AreaRateComputerFacade;
import app.bpartners.geojobs.service.event.DetectionRoofPropertiesRequestedService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Geometry;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FeatureRoofResultPropertiesComputer {
  private final GeometrySquareMeterArea geometrySquareMeterArea;
  private final ObjectMapper objectMapper;

  public HashMap<String, Object> apply(
      Feature feature,
      Geometry geometryUsedForAreaComputing,
      Geometry roofGeometryUsedForRateComputing,
      Collection<PolygonObjectType> detectedObjectPolygonGeometriesUsedForRateComputing) {

    var actualProperties = new HashMap<String, Object>();

    if (feature.getProperties() != null) {
      actualProperties.putAll(feature.getProperties());
    }

    actualProperties.put(
        "roof_area_in_m2", geometrySquareMeterArea.apply(geometryUsedForAreaComputing));

    var addresses = retrieveAddressesProperty(feature);
    actualProperties.put("addresses", addresses);

    var rateComputer =
        new AreaRateComputerFacade(
            roofGeometryUsedForRateComputing, detectedObjectPolygonGeometriesUsedForRateComputing);
    var usureRate = rateComputer.getUsureAreaRate();
    var humiditeRate = rateComputer.getHumidityAreaRate();
    var moisissureRate = rateComputer.getMoisissureAreaRate();
    var globalRateValue = rateComputer.getGlobalRate();
    var globalRateType = rateComputer.getRate();

    actualProperties.put("usure_rate", usureRate);
    actualProperties.put("humidite_rate", humiditeRate);
    actualProperties.put("moisissure_rate", moisissureRate);
    actualProperties.put("global_rate_value", globalRateValue);
    actualProperties.put("global_rate_type", globalRateType);

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

    return actualProperties;
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
