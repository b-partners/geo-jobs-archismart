package app.bpartners.geojobs.service.detection;

/**
 * Format of the detection API response/payload. Each {@link TileDetectorUrl} carries the version of
 * the API it points to so {@link HttpApiTileObjectDetector} can serialize the right payload and
 * parse the right response shape. Defaults to {@link #V2} when omitted from the configuration.
 */
public enum DetectionApiVersion {
  V1,
  V2
}
