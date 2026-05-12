package app.bpartners.geojobs.service.cityjson.texture;

import static app.bpartners.geojobs.file.FileWriter.createTempFile;
import static app.bpartners.geojobs.model.geometry.GeometryFactory.geometryFactory;
import static app.bpartners.geojobs.service.GeometrySquareMeterArea.LAMBERT_93;

import app.bpartners.geojobs.service.GeometrySquareMeterArea;
import app.bpartners.geojobs.service.cityjson.texture.model.RasterInfo;
import app.bpartners.geojobs.service.cityjson.texture.model.TextureInfo;
import app.bpartners.geojobs.service.cityjson.texture.model.TexturedCityJson;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import javax.imageio.ImageIO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.referencing.datum.PixelInCell;
import org.geotools.api.referencing.operation.MathTransform;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.jetbrains.annotations.NotNull;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.math.Vector3D;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CityJsonIOService {
  private final ObjectMapper objectMapper;
  private final GeometrySquareMeterArea geometrySquareMeterArea;

  public ObjectNode loadCityJson(File file) {
    try {
      return (ObjectNode) objectMapper.readTree(file);
    } catch (IOException e) {
      throw new IllegalStateException("Failed to read CityJSON file", e);
    }
  }

  public TextureInfo loadTexture(File tifFile) {
    RasterInfo rasterInfo = readRasterInfo(tifFile);
    return new TextureInfo(rasterInfo, tifFile);
  }

  public File toFile(TexturedCityJson texturedCityJson) {
    try {
      File file = createTempFile("textured-cityjson-", ".json");

      try (FileWriter writer = new FileWriter(file)) {
        writer.write(texturedCityJson.json().toString());
      }

      return file;

    } catch (IOException e) {
      throw new IllegalStateException("Failed to write JSON to file", e);
    }
  }

  public RasterInfo readRasterInfo(File tifFile) {

    BufferedImage image = readImage(tifFile);

    CoordinateReferenceSystem targetCrs = LAMBERT_93;

    double originX = 0;
    double originY = 0;
    double pixelWidth = 1;
    double pixelHeight = 1;

    try {
      GeoTiffReader reader = new GeoTiffReader(tifFile);

      GridCoverage2D coverage = reader.read(null);

      CoordinateReferenceSystem sourceCRS = coverage.getCoordinateReferenceSystem();
      if (sourceCRS == null) {
        throw new IllegalStateException("Missing CRS in GeoTIFF");
      }

      MathTransform transform = coverage.getGridGeometry().getGridToCRS(PixelInCell.CELL_CORNER);

      if (transform instanceof AffineTransform affine) {

        double gx = affine.getTranslateX();
        double gy = affine.getTranslateY();

        double sx = affine.getScaleX();
        double sy = affine.getScaleY();

        Vector3D origin = project(List.of(new Vector3D(gx, gy, 0)), sourceCRS, targetCrs).get(0);

        Vector3D stepX =
            project(List.of(new Vector3D(gx + sx, gy, 0)), sourceCRS, targetCrs).get(0);

        Vector3D stepY =
            project(List.of(new Vector3D(gx, gy + sy, 0)), sourceCRS, targetCrs).get(0);

        originX = origin.getX();
        originY = origin.getY();

        pixelWidth = stepX.getX() - originX;
        pixelHeight = stepY.getY() - originY;
      }

    } catch (Exception e) {
      throw new IllegalStateException("Failed to read GeoTIFF transform", e);
    }

    return new RasterInfo(
        originX,
        originY,
        pixelWidth,
        pixelHeight,
        0.0,
        0.0,
        image.getWidth(),
        image.getHeight(),
        targetCrs);
  }

  public List<Vector3D> project(
      List<Vector3D> vectors,
      CoordinateReferenceSystem sourceCrsCode,
      CoordinateReferenceSystem targetCrsCode) {
    return vectors.stream()
        .map(
            v -> {
              Point origin = geometryFactory.createPoint(new Coordinate(v.getX(), v.getY()));
              Point projected =
                  (Point) geometrySquareMeterArea.project(origin, sourceCrsCode, targetCrsCode);

              return new Vector3D(
                  projected.getCoordinate().x, projected.getCoordinate().y, v.getZ());
            })
        .toList();
  }

  @NotNull
  private static BufferedImage readImage(File tifFile) {
    BufferedImage image;
    try {
      image = ImageIO.read(tifFile);
    } catch (IOException e) {
      throw new IllegalStateException("Failed to read GeoTIFF image", e);
    }

    if (image == null) {
      throw new IllegalStateException("Could not read raster: " + tifFile.getAbsolutePath());
    }
    return image;
  }
}
