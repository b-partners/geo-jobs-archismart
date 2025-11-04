package app.bpartners.geojobs.service.gouv.fr.rnb.component.geometry;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import lombok.Getter;
import lombok.ToString;

@Getter
@JsonDeserialize(using = GeometryDeserializer.class)
@ToString
public class Geometry {
  private final app.bpartners.geojobs.endpoint.rest.model.Geometry.TypeEnum type;
  private final GeometryCoordinates coordinates;

  public Geometry(
      app.bpartners.geojobs.endpoint.rest.model.Geometry.TypeEnum type,
      GeometryCoordinates coordinates) {
    this.type = type;
    this.coordinates = coordinates;
  }

  public List<BigDecimal> getPointCoordinates() {
    if (coordinates instanceof PointCoordinates(List<BigDecimal> point)) {
      return point;
    }
    return null;
  }

  public List<List<List<BigDecimal>>> getPolygonCoordinates() {
    if (coordinates instanceof PolygonCoordinates(List<List<List<BigDecimal>>> polygon)) {
      return polygon;
    }
    return null;
  }

  public List<List<List<List<BigDecimal>>>> getMultiPolygonCoordinates() {
    if (coordinates
        instanceof MultiPolygonCoordinates(List<List<List<List<BigDecimal>>>> multiPolygon)) {
      return multiPolygon;
    }
    return null;
  }

  @Override
  public boolean equals(Object object) {
    if (object == null || getClass() != object.getClass()) return false;
    Geometry geometry = (Geometry) object;
    return getType() == geometry.getType()
        && Objects.equals(getCoordinates(), geometry.getCoordinates());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getType(), getCoordinates());
  }
}
