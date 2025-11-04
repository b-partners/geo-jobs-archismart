package app.bpartners.geojobs.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import app.bpartners.geojobs.conf.FacadeIT;
import app.bpartners.geojobs.endpoint.rest.model.Feature;
import app.bpartners.geojobs.endpoint.rest.model.FeatureGeometry;
import app.bpartners.geojobs.endpoint.rest.model.MultiPolygon;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class FeatureSurfaceServiceIT extends FacadeIT {
  @Autowired FeatureSurfaceService subject;

  @Test
  void can_calc_one_feature_surface_in_square_degree() {
    var expected = 6_000;

    var actual = subject.getAreaValue(oneFeature());

    assertEquals(expected, actual);
  }

  @Test
  void can_calc_many_feature_surface_in_square_degree() {
    var expected = 12_000;

    var actual = subject.getAreaValue(List.of(oneFeature(), oneFeature()));

    assertEquals(expected, actual);
  }

  private static Feature oneFeature() { // feature where the area is 6_000
    Feature feature = new Feature();
    var coordinates =
        List.of(
            List.of(
                List.of(
                    List.of(BigDecimal.valueOf(48.05622828269508), BigDecimal.valueOf(0)),
                    List.of(
                        BigDecimal.valueOf(24.028114141347547),
                        BigDecimal.valueOf(41.617914502878165)),
                    List.of(
                        BigDecimal.valueOf(-24.028114141347547),
                        BigDecimal.valueOf(41.617914502878165)),
                    List.of(
                        BigDecimal.valueOf(-48.05622828269508),
                        BigDecimal.valueOf(5.8851906145497036E-15)),
                    List.of(
                        BigDecimal.valueOf(-24.02811414134756),
                        BigDecimal.valueOf(-41.61791450287816)),
                    List.of(
                        BigDecimal.valueOf(24.02811414134751),
                        BigDecimal.valueOf(-41.617914502878186)),
                    List.of(BigDecimal.valueOf(48.05622828269508), BigDecimal.valueOf(0)))));
    MultiPolygon multiPolygon = new MultiPolygon().coordinates(coordinates);
    feature.setGeometry(new FeatureGeometry(multiPolygon));
    return feature;
  }
}
