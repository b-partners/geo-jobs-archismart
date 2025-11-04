package app.bpartners.geojobs.model.parcelization.area;

import static app.bpartners.geojobs.model.geometry.area.MetricArea.MetricAreaUnit.are;
import static app.bpartners.geojobs.model.geometry.area.MetricArea.MetricAreaUnit.ha;
import static app.bpartners.geojobs.model.geometry.area.MetricArea.MetricAreaUnit.km2;
import static app.bpartners.geojobs.model.geometry.area.MetricArea.MetricAreaUnit.m2;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.bpartners.geojobs.model.geometry.area.MetricArea;
import app.bpartners.geojobs.model.geometry.area.SquareDegree;
import org.junit.jupiter.api.Test;

class MetricAreaTest {

  @Test
  void equals() {
    assertEquals(0, new MetricArea(2, m2).compareTo(new MetricArea(2, m2)));
    assertEquals(0, new MetricArea(2_00, m2).compareTo(new MetricArea(2, are)));
    assertEquals(0, new MetricArea(2_00_00, m2).compareTo(new MetricArea(2, ha)));
    assertEquals(0, new MetricArea(2_00_00_00, m2).compareTo(new MetricArea(2, km2)));

    assertEquals(0, new MetricArea(2_00, are).compareTo(new MetricArea(2, ha)));
    assertEquals(0, new MetricArea(2_00, ha).compareTo(new MetricArea(2, km2)));
  }

  @Test
  void lessThan() {
    assertTrue(new MetricArea(2, m2).compareTo(new MetricArea(3, m2)) < 0);
    assertTrue(new MetricArea(2_00, m2).compareTo(new MetricArea(3, are)) < 0);
  }

  @Test
  void unsupported() {
    var a1 = new MetricArea(2, m2);
    var a2 = new SquareDegree(0.1);
    assertThrows(UnsupportedOperationException.class, () -> a1.compareTo(a2));
  }
}
