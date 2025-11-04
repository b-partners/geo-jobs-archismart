package app.bpartners.geojobs.model.geometry.quadrilateral.model;

import static app.bpartners.geojobs.model.geometry.GeometryFactory.geometryFactory;
import static app.bpartners.geojobs.model.geometry.quadrilateral.model.ContinuationOrientation.lengthOnly;
import static app.bpartners.geojobs.model.geometry.quadrilateral.model.ContinuationOrientation.lengthOrWidth;
import static java.lang.Math.PI;

import app.bpartners.geojobs.model.geometry.HaveAnglesSameDirection;
import app.bpartners.geojobs.model.geometry.IntXY;
import app.bpartners.geojobs.model.geometry.LineInt;
import app.bpartners.geojobs.model.geometry.TwoLineInt;
import app.bpartners.geojobs.model.geometry.route.ContinuationConf;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Coordinate;

@Slf4j
public record OrientedQuadrilateral(
    Quadrilateral quadrilateral, ContinuationOrientation continuationOrientation) {

  public Optional<OrientedQuadrilateral> continueWith(
      OrientedQuadrilateral that, ContinuationConf continuationConf) {
    try {
      return fallibleContinueWith(that, continuationConf);
    } catch (Exception e) {
      log.error(String.format("Continuation failed: this=%s , that=%s", this, that), e);
      return Optional.empty();
    }
  }

  public Optional<OrientedQuadrilateral> fallibleContinueWith(
      OrientedQuadrilateral that, ContinuationConf continuationConf) {
    var origin = geometryFactory.createPoint(new Coordinate(0, 0));
    var distanceThreshold = continuationConf.distanceThreshold();
    if (that.quadrilateral.centroid().distance(origin) < quadrilateral.centroid().distance(origin)
        & isCloseEnoughWith(that, distanceThreshold)) {
      return that.fallibleContinueWith(this, continuationConf);
    }
    if (!isCloseEnoughWith(that, distanceThreshold)
        || !hasContinuableDirectionWith(that, continuationConf)) {
      return Optional.empty();
    }

    var continuedQuadrilateral =
        Quadrilateral.fromIntXYCoordinates(continuationCoordinatesFrom(that));
    return Optional.of(new OrientedQuadrilateral(continuedQuadrilateral, continuationOrientation));
  }

  private Set<IntXY> continuationCoordinatesFrom(OrientedQuadrilateral that) {
    var twoShortestBetween = getTwoSortestBetween(that);
    var shortestBetween1 = twoShortestBetween.first();
    var shortestBetween2 = twoShortestBetween.second();
    return Set.of(
        shortestBetween1.a(), shortestBetween1.b(), shortestBetween2.a(), shortestBetween2.b());
  }

  private TwoLineInt getTwoSortestBetween(OrientedQuadrilateral that) {
    var removableCoordinates1 = this.getRemovableCoordinates();
    var removableCoordinates2 = that.getRemovableCoordinates();
    var shortestBetween1 = shortestLineFrom(removableCoordinates1, removableCoordinates2);

    removableCoordinates1.remove(shortestBetween1.a());
    removableCoordinates2.remove(shortestBetween1.b());
    var shortestBetween2 = shortestLineFrom(removableCoordinates1, removableCoordinates2);

    return new TwoLineInt(shortestBetween1, shortestBetween2);
  }

  private HashSet<IntXY> getRemovableCoordinates() {
    return new HashSet<>(
        Arrays.stream(quadrilateral.polygon().getCoordinates()).map(IntXY::new).toList());
  }

  private static LineInt shortestLineFrom(Set<IntXY> xy1, Set<IntXY> xy2) {
    LineInt res = null;
    var minLength = Double.MAX_VALUE;

    for (var c1 : xy1) {
      for (var c2 : xy2) {
        var length = new LineInt(c1, c2).length();
        if (length < minLength) {
          minLength = length;
          res = new LineInt(c1, c2);
        }
      }
    }
    return res;
  }

  private boolean hasContinuableDirectionWith(
      OrientedQuadrilateral that, ContinuationConf continuationConf) {
    return switch (continuationOrientation) {
      case lengthOnly ->
          switch (that.continuationOrientation) {
            case lengthOnly -> hasContinuableDirection(that, continuationConf, lengthOnly);
            case lengthOrWidth -> hasContinuableDirection(that, continuationConf, lengthOrWidth);
          };
      case lengthOrWidth ->
          switch (that.continuationOrientation) {
            case lengthOnly -> that.hasContinuableDirectionWith(this, continuationConf);
            case lengthOrWidth -> false;
          };
    };
  }

  private boolean hasContinuableDirection(
      OrientedQuadrilateral that,
      ContinuationConf continuationConf,
      ContinuationOrientation orientation) {
    var twoShortestBetween = getTwoSortestBetween(that);
    var shortestBetween1 = twoShortestBetween.first();
    var shortestBetween2 = twoShortestBetween.second();
    var areShortestBetweenContinuable =
        areShortestBetweenContinuable(shortestBetween1, shortestBetween2, continuationConf)
            || that.areShortestBetweenContinuable(
                shortestBetween1, shortestBetween2, continuationConf);

    var directionThreshold = directionThreshold(continuationConf, shortestBetween1, orientation);
    var haveAnglesSameDirection = new HaveAnglesSameDirection(directionThreshold);
    return haveAnglesSameDirection.test(quadrilateral.angle(), that.quadrilateral.angle())
        && areShortestBetweenContinuable;
  }

  private static double directionThreshold(
      ContinuationConf continuationConf,
      LineInt shortestBetween,
      @Nullable ContinuationOrientation orientation) {
    double minDirectionThreshold = continuationConf.minDirectionThreshold();
    double maxDirectionThreshold =
        lengthOrWidth.equals(orientation)
            ? PI / 2
            : continuationConf
                .maxDirectionThreshold(); // PI/2 because the lines and the pathway are nearly
    // perpendicular
    var directionRatio = 1 - shortestBetween.length() / continuationConf.distanceThreshold();
    return minDirectionThreshold + (maxDirectionThreshold - minDirectionThreshold) * directionRatio;
  }

  private boolean areShortestBetweenContinuable(
      LineInt shortestBetween1, LineInt shortestBetween2, ContinuationConf continuationConf) {

    var directionThreshold1 = directionThreshold(continuationConf, shortestBetween1, null);
    var haveAnglesSameDirection1 = new HaveAnglesSameDirection(directionThreshold1);

    var directionThreshold2 = directionThreshold(continuationConf, shortestBetween2, null);
    var haveAnglesSameDirection2 = new HaveAnglesSameDirection(directionThreshold2);

    return haveAnglesSameDirection1.test(quadrilateral.angle(), shortestBetween1.angle())
        || haveAnglesSameDirection2.test(quadrilateral.angle(), shortestBetween2.angle());
  }

  private boolean isCloseEnoughWith(OrientedQuadrilateral that, double distanceThreshold) {
    var distance = quadrilateral.polygon().distance(that.quadrilateral.polygon());
    return distance < distanceThreshold;
  }
}
