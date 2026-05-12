package app.bpartners.geojobs.service.lidar;

import static app.bpartners.geojobs.service.GeometrySquareMeterArea.*;
import static java.util.stream.Collectors.toSet;

import app.bpartners.geojobs.model.lidar.planes.model.DelimitedRoofPoints;
import app.bpartners.geojobs.model.lidar.planes.model.LasRoofDelimitationType;
import app.bpartners.geojobs.model.lidar.planes.model.RoofPointsDelimitationTransformer;
import app.bpartners.geojobs.service.GeometrySquareMeterArea;
import app.bpartners.geojobs.service.lidar.api.LidarApiFacade;
import app.bpartners.geojobs.service.lidar.api.SwissBoundaryChecker;
import java.util.*;
import java.util.function.BiFunction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
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
      LidarApiFacade lidarApi,
      GeometrySquareMeterArea projector,
      SwissBoundaryChecker swissBoundaryChecker) {
    this.lidarApi = lidarApi;
    this.projector = projector;
    this.swissBoundaryChecker = swissBoundaryChecker;
    this.pointsExtractorFromOneUrl = new LasRoofPointsExtractorFromOneUrl(lidarApi);
  }

  private static final double ROOF_FACES_BUFFER = 3;
  private static final int MIN_BATIMENT_POINTS_COUNT = 10;

  @Override
  public PointsExtractionResult apply(LasRoofDelimitationType type, Set<Geometry> roofsEPSG4326) {
    try {
      var lidarFilesUrl = lidarApi.getUniqueLidarFilesUrls(roofsEPSG4326);
      if (lidarFilesUrl.isEmpty()) {
        return new PointsExtractionResult(new HashMap<>());
      }

      var delimitations = emptyDelimitedPoints(type, roofsEPSG4326);
      var pointsPerFiles = getPointsFromFiles(lidarFilesUrl, delimitations);
      var all = new HashSet<>(pointsPerFiles);
      all.add(new HashSet<>(delimitations.values()));

      var merged = mergeSameRoofEnvelope(all);

      validateRoofPointsCount(merged);

      return new PointsExtractionResult(merged);
    } catch (Exception e) {
      log.error("Failed to retrieve lidar data", e);
      throw e;
    }
  }

  private static void validateRoofPointsCount(Map<Envelope, DelimitedRoofPoints> delimitedPoints) {
    for (var delimitation : delimitedPoints.values()) {
      var roofPoints = delimitation.getPoints();
      if (roofPoints.size() < MIN_BATIMENT_POINTS_COUNT) {
        throw new IllegalStateException(
            "Roof found but no BATIMENT points detected for one of the buildings. "
                + "Lidar data exists but roof analysis failed for this roof.");
      }
    }
  }

  private static Map<Envelope, DelimitedRoofPoints> mergeSameRoofEnvelope(
      Set<Set<DelimitedRoofPoints>> roofsDataFromFiles) {
    Map<Envelope, DelimitedRoofPoints> merged = new HashMap<>();
    for (var roofDataFromOneFile : roofsDataFromFiles) {
      for (var delimitation : roofDataFromOneFile) {
        var key = delimitation.getOriginalInEPSG4336().getEnvelopeInternal();
        merged.merge(key, delimitation, DelimitedRoofPoints::merge);
      }
    }
    return merged;
  }

  private Set<Set<DelimitedRoofPoints>> getPointsFromFiles(
      Map<String, Set<Geometry>> filesUrls, Map<Envelope, DelimitedRoofPoints> delimitations) {
    return filesUrls.entrySet().parallelStream()
        .map(
            entry -> {
              var fileUrl = entry.getKey();
              var roofsInLocalCRS = entry.getValue();
              var toProcess = getDelimitationsToProcess(delimitations, roofsInLocalCRS);
              return pointsExtractorFromOneUrl.apply(fileUrl, toProcess);
            })
        .collect(toSet());
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
}
