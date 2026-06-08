package app.bpartners.geojobs.service.cityjson.texture.model;

import lombok.Builder;

@Builder(toBuilder = true)
public record RasterInfo(
    int width, int height, int zoom, int tileX, int tileY, int tileImageSizePx) {}
