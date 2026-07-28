package app.bpartners.geojobs.endpoint.rest.controller.v1;

import static java.util.UUID.randomUUID;

import app.bpartners.geojobs.endpoint.rest.V1RestController;
import app.bpartners.geojobs.endpoint.rest.controller.v1.mapper.FeatureMapper;
import app.bpartners.geojobs.endpoint.rest.model.Feature;
import app.bpartners.geojobs.service.ParcelService;
import java.util.Collection;
import java.util.List;
import lombok.AllArgsConstructor;
import org.locationtech.jts.geom.Polygon;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@V1RestController
@AllArgsConstructor
public class ParcelizationController {
  private final FeatureMapper featureMapper;
  private final ParcelService parcelService;

  @PutMapping("/parcelization")
  public List<Feature> processFeatureParcelization(
      @RequestBody List<Feature> features,
      @RequestParam(name = "referenceZoom", required = false) Integer referenceZoom,
      @RequestParam(name = "targetZoom", required = false) Integer targetZoom,
      @RequestParam(name = "maxParcelAreaAtReferenceZoom", required = false)
          Double maxParcelAreaAtReferenceZoom) {
    List<Polygon> polygons =
        features.stream().map(featureMapper::toDomainList).flatMap(List::stream).toList();
    String polygonsId = randomUUID().toString();
    return polygons.stream()
        .map(
            polygon ->
                parcelService.parcelizeFeature(
                    polygon, referenceZoom, targetZoom, maxParcelAreaAtReferenceZoom, polygonsId))
        .flatMap(Collection::stream)
        .toList();
  }
}
