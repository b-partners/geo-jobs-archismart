package app.bpartners.geojobs.service.cityjson.model.object.io;

import app.bpartners.geojobs.service.cityjson.model.object.Transform;

public final class VertexResolver {
  private final long[][] vertices;
  private final double[] scale;
  private final double[] translate;

  public VertexResolver(long[][] vertices, Transform transform) {
    this.vertices = vertices;
    this.scale = transform.getScale();
    this.translate = transform.getTranslate();
  }

  public double[] get(int index) {
    long[] v = vertices[index];
    return new double[] {
      v[0] * scale[0] + translate[0], v[1] * scale[1] + translate[1], v[2] * scale[2] + translate[2]
    };
  }
}
