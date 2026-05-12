package app.bpartners.geojobs.service;

import static app.bpartners.geojobs.endpoint.rest.model.Feature.TypeEnum.FEATURE;

import app.bpartners.geojobs.endpoint.rest.model.*;
import app.bpartners.geojobs.model.exception.NotImplementedException;
import java.util.ArrayList;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SynchronousDetectionValidator implements Function<CreateDetection, CreateDetection> {

  @Override
  public CreateDetection apply(CreateDetection createDetection) {
    if (createDetection != null
        && createDetection.getGeoJsonZone() != null
        && createDetection.getGeoJsonZone().size() != 1) {
      throw new NotImplementedException(
          "Only unique feature can be processed synchronously for now, meanwhile given are "
              + createDetection.getGeoJsonZone().size());
    }
    if (createDetection.getGeoJsonZone() != null) {
      createDetection
          .getGeoJsonZone()
          .forEach(
              feature -> {
                var properties = feature.getProperties();
                if (properties != null) {
                  properties.remove("id");
                }
              });
    }
    var fixFeatures = new ArrayList<Feature>();
    if (createDetection.getGeoJsonZone() != null && createDetection.getGeoJsonZone().size() == 1) {
      var uniqueFeature = createDetection.getGeoJsonZone().getFirst();
      var actualInstance = uniqueFeature.getGeometry().getActualInstance();
      if (actualInstance instanceof MultiPolygon multiPolygon) {
        var polygonCoordinates = multiPolygon.getCoordinates().getFirst();
        fixFeatures.add(
            new Feature()
                .type(FEATURE)
                .properties(uniqueFeature.getProperties())
                .geometry(
                    new FeatureGeometry(
                        new Polygon()
                            .type(Polygon.TypeEnum.POLYGON)
                            .coordinates(polygonCoordinates))));
      }
    }
    if (fixFeatures.isEmpty()) {
      return createDetection;
    }
    // TODO: set immutable through a CreateDetection instance copy
    return createDetection.geoJsonZone(fixFeatures);
  }
}
