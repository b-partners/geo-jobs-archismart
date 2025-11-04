package app.bpartners.geojobs.repository.model.community;

import static app.bpartners.geojobs.repository.model.detection.DetectableType.LINE;
import static app.bpartners.geojobs.repository.model.detection.DetectableType.PISCINE;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class CommunityDetectableObjectTypeTest {
  @Test
  void getter_setter_test() {
    CommunityDetectableObjectType value = new CommunityDetectableObjectType();
    value.setId("123");
    value.setType(PISCINE);
    value.setCommunityAuthorizationId("communityId");

    assertEquals("123", value.getId());
    assertEquals("communityId", value.getCommunityAuthorizationId());
    assertEquals(PISCINE, value.getType());
  }

  @Test
  void equals_and_hashcode_test() {
    CommunityDetectableObjectType lhsValue = new CommunityDetectableObjectType();
    lhsValue.setId("123");
    lhsValue.setType(LINE);

    CommunityDetectableObjectType rhsValue = new CommunityDetectableObjectType();
    rhsValue.setId("123");
    rhsValue.setType(LINE);

    assertEquals(lhsValue, rhsValue);
    assertEquals(lhsValue.hashCode(), rhsValue.hashCode());
  }
}
