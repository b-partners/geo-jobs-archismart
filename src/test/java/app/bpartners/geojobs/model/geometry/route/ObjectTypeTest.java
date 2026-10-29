package app.bpartners.geojobs.model.geometry.route;

import static app.bpartners.geojobs.model.geometry.quadrilateral.model.ContinuationOrientation.lengthOrWidth;
import static app.bpartners.geojobs.model.geometry.route.ObjectType.lumineux;
import static app.bpartners.geojobs.model.geometry.route.ObjectType.obstacle;
import static app.bpartners.geojobs.model.geometry.route.ObjectType.routeTypeFrom;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class ObjectTypeTest {

  @Test
  void lumineux_label_resolves_to_a_route_type_like_obstacle() {
    assertEquals(lumineux, routeTypeFrom("lumineux"));
    assertEquals(lumineux, routeTypeFrom("LUMINEUX"));
    assertEquals(obstacle, routeTypeFrom("obstacle"));
    assertEquals(lengthOrWidth, lumineux.continuationOrientation());
  }

  @Test
  void unknown_label_is_still_rejected() {
    assertThrows(IllegalArgumentException.class, () -> routeTypeFrom("unknown_label"));
  }
}
