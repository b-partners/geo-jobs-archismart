package app.bpartners.geojobs.repository.model.community;

import static org.junit.jupiter.api.Assertions.*;

import app.bpartners.geojobs.endpoint.rest.model.MultiPolygon;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class CommunityAuthorizedZoneTest {

  @Test
  void getter_setter_test() {
    CommunityAuthorizedZone zone = new CommunityAuthorizedZone();
    zone.setId("zone1");
    zone.setName("Zone1");
    zone.setMultiPolygon(multipolygon());
    zone.setCommunityAuthorizationId("communityId");

    assertEquals("zone1", zone.getId());
    assertEquals("Zone1", zone.getName());
    assertEquals("communityId", zone.getCommunityAuthorizationId());
    assertEquals(multipolygon(), zone.getMultiPolygon());
  }

  @Test
  void equals_and_hashcode_test() {
    CommunityAuthorizedZone zone1 = new CommunityAuthorizedZone();
    zone1.setId("zone1");
    zone1.setName("Zone1");
    zone1.setMultiPolygon(multipolygon());

    CommunityAuthorizedZone zone2 = new CommunityAuthorizedZone();
    zone2.setId("zone1");
    zone2.setName("Zone1");
    zone2.setMultiPolygon(multipolygon());

    assertEquals(zone1, zone2);
    assertEquals(zone1.hashCode(), zone2.hashCode());
  }

  private static MultiPolygon multipolygon() {
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
    return new MultiPolygon().coordinates(coordinates);
  }
}
