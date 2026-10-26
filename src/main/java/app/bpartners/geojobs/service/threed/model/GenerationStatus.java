package app.bpartners.geojobs.service.threed.model;

public enum GenerationStatus {
  NORMAL,
  /** Generated, but part of the result is degraded, see {@link Lrg#getMessage()}. */
  DEGRADED,
  /** No lidar point at all covers the requested footprint: nothing was generated. */
  NO_LIDAR,
  FAILED
}
