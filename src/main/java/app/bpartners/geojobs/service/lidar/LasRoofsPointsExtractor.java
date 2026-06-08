package app.bpartners.geojobs.service.lidar;

import static app.bpartners.geojobs.service.GeometrySquareMeterArea.*;

import app.bpartners.geojobs.model.lidar.planes.model.DelimitedRoofPoints;
import app.bpartners.geojobs.model.lidar.planes.model.LasRoofDelimitationType;
import app.bpartners.geojobs.model.lidar.planes.model.RoofPointsDelimitationTransformer;
import app.bpartners.geojobs.service.GeometrySquareMeterArea;
import app.bpartners.geojobs.service.lidar.api.LasIndexApi;
import app.bpartners.geojobs.service.lidar.api.LidarApiFacade;
import app.bpartners.geojobs.service.lidar.api.SwissBoundaryChecker;
import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.util.GeometryFixer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Builder
@Component
@RequiredArgsConstructor
public class LasRoofsPointsExtractor
    implements BiFunction<LasRoofDelimitationType, Set<Geometry>, PointsExtractionResult> {
  private final LidarApiFacade lidarApi;
  private final GeometrySquareMeterArea projector;
  private final SwissBoundaryChecker swissBoundaryChecker;
  private final LasRoofPointsExtractorFromOneUrl pointsExtractorFromOneUrl;

  @Autowired
  public LasRoofsPointsExtractor(
      LasIndexApi lasIndexApi,
      LidarApiFacade lidarApi,
      GeometrySquareMeterArea projector,
      SwissBoundaryChecker swissBoundaryChecker) {
    this.lidarApi = lidarApi;
    this.projector = projector;
    this.swissBoundaryChecker = swissBoundaryChecker;
    this.pointsExtractorFromOneUrl = new LasRoofPointsExtractorFromOneUrl(lidarApi, lasIndexApi);
  }

  private static final double ROOF_FACES_BUFFER = 0;
  private static final int MIN_BATIMENT_POINTS_COUNT = 3;

  @Override
  public PointsExtractionResult apply(LasRoofDelimitationType type, Set<Geometry> roofsEPSG4326) {
    try {
      Set<Geometry> roofsEPSG4326Validated =
          roofsEPSG4326.stream().map(this::validateAndFix).collect(Collectors.toSet());
      var lidarFilesUrl = lidarApi.getUniqueLidarFilesUrls(roofsEPSG4326Validated);

      if (lidarFilesUrl.isEmpty()) {
        return new PointsExtractionResult(new HashMap<>());
      }

      var delimitations = emptyDelimitedPoints(type, roofsEPSG4326Validated);
      var pointsPerFiles = getPointsFromFiles(lidarFilesUrl, delimitations);
      var all = new ArrayList<>(pointsPerFiles);
      all.add(new HashSet<>(delimitations.values()));
      var merged = mergeSameRoofEnvelope(all);

      validateRoofPointsCount(merged);

      return new PointsExtractionResult(merged);
    } catch (Exception e) {
      log.error("Failed to retrieve lidar data", e);
      throw e;
    }
  }

  private Geometry validateAndFix(Geometry geometry) {
    log.info("Check and validate geometry={}", geometry);
    if (geometry instanceof MultiPolygon multiPolygon) {
      Polygon[] polygons =
          IntStream.range(0, multiPolygon.getNumGeometries())
              .mapToObj(i -> (Polygon) multiPolygon.getGeometryN(i))
              .map(p -> p.isValid() ? p : (Polygon) GeometryFixer.fix(p))
              .toArray(Polygon[]::new);
      return geometry.getFactory().createMultiPolygon(polygons);
    }

    if (geometry instanceof Polygon polygon) {
      return polygon.isValid() ? polygon : GeometryFixer.fix(polygon);
    }

    return geometry;
  }

  private static void validateRoofPointsCount(Map<Envelope, DelimitedRoofPoints> delimitedPoints) {
    for (var delimitation : delimitedPoints.values()) {
      var roofPoints = delimitation.getPoints();
      log.info("RoofPoints (delimitation) size = {}", roofPoints.size());
      if (roofPoints.size() < MIN_BATIMENT_POINTS_COUNT) {
        throw new IllegalStateException(
            "Roof found but no BATIMENT points detected for one of the buildings. "
                + "Lidar data exists but roof analysis failed for this roof.");
      }
    }
  }

  private static Map<Envelope, DelimitedRoofPoints> mergeSameRoofEnvelope(
      ArrayList<Set<DelimitedRoofPoints>> roofsDataFromFiles) {
    Map<Envelope, DelimitedRoofPoints> merged = new HashMap<>();

    for (var roofDataFromOneFile : roofsDataFromFiles) {
      for (var delimitation : roofDataFromOneFile) {
        var key = delimitation.getOriginalInEPSG4336().getEnvelopeInternal();
        merged.merge(key, delimitation, DelimitedRoofPoints::merge);
      }
    }
    return merged;
  }

  private List<Set<DelimitedRoofPoints>> getPointsFromFiles(
      Map<String, Set<Geometry>> filesUrls, Map<Envelope, DelimitedRoofPoints> delimitations) {
    return filesUrls.entrySet().parallelStream()
        .map(
            entry -> {
              var fileUrl = entry.getKey();
              var roofsInLocalCRS = entry.getValue();
              var toProcess = getDelimitationsToProcess(delimitations, roofsInLocalCRS);
              return pointsExtractorFromOneUrl.apply(fileUrl, toProcess);
            })
        .collect(Collectors.toList());
  }

  private Set<DelimitedRoofPoints> getDelimitationsToProcess(
      Map<Envelope, DelimitedRoofPoints> delimitations, Set<Geometry> roofsInLocalCRS) {
    Set<DelimitedRoofPoints> toProcess = new HashSet<>();

    for (var delimitation : delimitations.values()) {
      var envelope = delimitation.getGlobalEnvelope();
      for (var roof : roofsInLocalCRS) {
        var roofEnvelope = roof.getEnvelopeInternal();
        if (envelope.equals(roofEnvelope)) {
          toProcess.add(delimitation);
          break;
        }
      }
    }

    return toProcess;
  }

  private Map<Envelope, DelimitedRoofPoints> emptyDelimitedPoints(
      LasRoofDelimitationType type, Set<Geometry> roofsEPSG4326) {
    Map<Envelope, DelimitedRoofPoints> delimitations = new HashMap<>();
    var transformer = new RoofPointsDelimitationTransformer(ROOF_FACES_BUFFER);

    for (var roofEPSG4326 : roofsEPSG4326) {
      var envelope = roofEPSG4326.getEnvelopeInternal();
      var roofInLocalCRS = projectToLocalCRS(roofEPSG4326);
      var delimitedRoofPoints =
          new DelimitedRoofPoints(type, roofEPSG4326, roofInLocalCRS, transformer);

      delimitations.put(envelope, delimitedRoofPoints);
    }

    return delimitations;
  }

  private Geometry projectToLocalCRS(Geometry roofEPSG4326) {
    if (swissBoundaryChecker.isGeometryInSwiss(roofEPSG4326)) {
      return projector.project(roofEPSG4326, WGS84, EPSG_2056);
    }
    return projector.project(roofEPSG4326, WGS84, LAMBERT_93);
  }

  private static Envelope normalize(Envelope envelope) {
    return new Envelope(
        round(envelope.getMinX()),
        round(envelope.getMaxX()),
        round(envelope.getMinY()),
        round(envelope.getMaxY()));
  }

  private static double round(double value) {
    return Math.round(value * 1_000_000d) / 1_000_000d;
  }
}
