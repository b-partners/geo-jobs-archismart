package app.bpartners.geojobs.service.cityjson.texture.model;

import lombok.RequiredArgsConstructor;
import org.citygml4j.cityjson.model.geometry.Vertex;

@RequiredArgsConstructor
public class Transform {
  private final Vertex scale;
  private final Vertex translate;

  public Vertex apply(Vertex coordinate) {
    return Vertex.of(
        coordinate.getX() * scale.getX() + translate.getX(),
        coordinate.getY() * scale.getY() + translate.getY(),
        coordinate.getZ() * scale.getZ() + translate.getZ());
  }
}
