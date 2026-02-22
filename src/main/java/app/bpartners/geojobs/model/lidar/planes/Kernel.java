package app.bpartners.geojobs.model.lidar.planes;

import static app.bpartners.geojobs.model.lidar.planes.algorithm.Vector3DUtils.hasPerpendicularDirection;
import static app.bpartners.geojobs.model.lidar.planes.algorithm.Vector3DUtils.hasSameDirectionIgnoringOrientation;

import app.bpartners.geojobs.model.lidar.LasPointGeometry;
import java.security.SecureRandom;
import java.util.*;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.math.Vector3D;

@Builder
@RequiredArgsConstructor
public class Kernel {
  private final Conf conf;
  @Getter private final KernelChains chains;

  public static Optional<Kernel> from(
      Collection<LasPointGeometry> points, Conf conf, SecureRandom random) {
    var optionalChains = getKernelChains(new ArrayList<>(points), conf, random);
    return optionalChains.map(chains -> Kernel.builder().conf(conf).chains(chains).build());
  }

  public int size() {
    return this.chains.size();
  }

  private static Optional<KernelChains> getKernelChains(
      List<LasPointGeometry> points, Conf conf, SecureRandom random) {
    for (int i = 0; i < conf.attempts(); i++) {
      var p1 = points.get(random.nextInt(points.size()));
      var candidates = getNeighborsCandidates(p1, points, conf);
      if (candidates.isEmpty()) continue;

      var p2Candidates = getP2Candidates(candidates, conf, random);
      for (var p2Candidate : p2Candidates) {
        var main = getMainKernelChain(p2Candidate, points, conf, random);
        var optionalPerpendicular = getPerpendicularKernelChain(main, points, conf, random);
        if (optionalPerpendicular.isPresent()) {
          return Optional.of(
              new KernelChains(conf.degEpsilon(), main, optionalPerpendicular.get()));
        }
      }
    }
    return Optional.empty();
  }

  private static List<Candidate> getNeighborsCandidates(
      LasPointGeometry p1, List<LasPointGeometry> points, Conf conf) {
    return points.stream()
        .filter(point -> point != p1 && p1.squaredDistance(point) < conf.squaredThreshold())
        .map(point -> Candidate.from(p1, point))
        .filter(candidate -> candidate.norm() > conf.minVectorNorm())
        .toList();
  }

  private static List<Candidate> getP2Candidates(
      List<Candidate> neighbors, Conf conf, SecureRandom random) {
    if (neighbors.size() <= conf.attempts()) {
      return neighbors;
    }

    Set<Candidate> selected = new HashSet<>();
    for (int i = 0; i < conf.attempts(); i++) {
      selected.add(neighbors.get(random.nextInt(neighbors.size())));
    }
    return new ArrayList<>(selected);
  }

  private static KernelChain getMainKernelChain(
      Candidate p2Candidate, List<LasPointGeometry> points, Conf conf, SecureRandom random) {
    var chain = new KernelChain(p2Candidate.a(), p2Candidate.b());
    var direction = p2Candidate.vector();

    while (chain.size() < conf.maxLength()) {
      var last = chain.getFurthestPoints().getLast();
      var candidates = getDirectedCandidates(points, chain.getPoints(), last, direction, conf);

      if (candidates.isEmpty()) break;

      var next = candidates.get(random.nextInt(candidates.size()));
      chain.add(next);
    }

    return chain;
  }

  private static List<LasPointGeometry> getDirectedCandidates(
      List<LasPointGeometry> points,
      Set<LasPointGeometry> used,
      LasPointGeometry last,
      Vector3D direction,
      Conf conf) {
    List<LasPointGeometry> candidates = new ArrayList<>();
    for (var p : points) {
      if (used.contains(p)) continue;
      if (last.squaredDistance(p) >= conf.squaredThreshold()) continue;

      // 0° or 180°
      var vector = new Vector3D(last.getCoordinate(), p.getCoordinate());
      if (!hasSameDirectionIgnoringOrientation(direction, vector, conf.degEpsilon())) {
        continue;
      }

      candidates.add(p);
    }

    return candidates;
  }

  private static Optional<KernelChain> getPerpendicularKernelChain(
      KernelChain main, List<LasPointGeometry> points, Conf conf, SecureRandom random) {
    var optionalChain = getStartPerpendicularChain(main, points, conf);
    if (optionalChain.isEmpty()) return Optional.empty();

    var chain = optionalChain.get();

    var direction = chain.getDirection();
    while (chain.size() < conf.maxLength()) {
      var last = chain.getFurthestPoints().getLast();
      var candidates = getDirectedCandidates(points, chain.getPoints(), last, direction, conf);

      if (candidates.isEmpty()) break;

      var next = candidates.get(random.nextInt(candidates.size()));
      chain.add(next);
    }

    return Optional.of(chain);
  }

  private static Optional<KernelChain> getStartPerpendicularChain(
      KernelChain main, List<LasPointGeometry> points, Conf conf) {
    var set = new HashSet<>(points);
    var mainPointsCandidates = main.getFurthestPoints();
    for (var p3 : set) {
      if (main.getPoints().contains(p3)) continue;
      var optionalP3Pair =
          mainPointsCandidates.stream()
              .filter(
                  mainPoint -> {
                    if (mainPoint.squaredDistance(p3) > conf.squaredThreshold()) return false;
                    var p3Direction = new Vector3D(mainPoint.getCoordinate(), p3.getCoordinate());
                    if (p3Direction.length() < conf.minVectorNorm()) return false;
                    return hasPerpendicularDirection(
                        p3Direction, main.getDirection(), conf.degEpsilon());
                  })
              .findFirst();

      if (optionalP3Pair.isPresent()) {
        return Optional.of(new KernelChain(optionalP3Pair.get(), p3));
      }
    }
    return Optional.empty();
  }

  private record Candidate(LasPointGeometry a, LasPointGeometry b, Vector3D vector, double norm) {
    static Candidate from(LasPointGeometry p1, LasPointGeometry point) {
      var v1 = new Vector3D(p1.getCoordinate(), point.getCoordinate());
      var norm = v1.length();
      return new Candidate(p1, point, v1, norm);
    }
  }

  @RequiredArgsConstructor
  public static class KernelChain {
    private Vector3D direction;
    @Getter private final Set<LasPointGeometry> points;
    private List<LasPointGeometry> furthestPoints;

    public KernelChain(LasPointGeometry p1, LasPointGeometry p2) {
      this(new HashSet<>(Set.of(p1, p2)));
    }

    public Vector3D getDirection() {
      if (direction == null) {
        var furthestPointsValue = this.getFurthestPoints();
        direction =
            new Vector3D(
                furthestPointsValue.getFirst().getCoordinate(),
                furthestPointsValue.getLast().getCoordinate());
      }

      return direction;
    }

    public int size() {
      return points.size();
    }

    void add(LasPointGeometry point) {
      this.points.add(point);
      this.direction = null;
      this.furthestPoints = null;
    }

    public List<LasPointGeometry> getFurthestPoints() {
      if (furthestPoints == null) {
        furthestPoints = computeFurthestPoints(this.points);
      }
      return furthestPoints;
    }

    public static List<LasPointGeometry> computeFurthestPoints(Set<LasPointGeometry> points) {
      var list = new ArrayList<>(points);
      if (points.size() < 3) return list;

      double maxDist2 = -1;
      LasPointGeometry p1 = null;
      LasPointGeometry p2 = null;

      for (int i = 0; i < points.size(); i++) {
        var p1Candidate = list.get(i);
        for (int j = i + 1; j < points.size(); j++) {
          var p2Candidate = list.get(j);
          double dist2 = p1Candidate.squaredDistance(p2Candidate);

          if (dist2 > maxDist2) {
            maxDist2 = dist2;
            p1 = p1Candidate;
            p2 = p2Candidate;
          }
        }
      }

      assert p2 != null;
      return List.of(p1, p2);
    }
  }

  public record KernelChains(double degEpsilon, KernelChain main, KernelChain perpendicular) {
    public int size() {
      return Math.min(main.size(), perpendicular.size());
    }

    public List<LasPointGeometry> getOrthogonalTriplet() {
      var mainFurthestPoints = main.getFurthestPoints();
      var p1 = mainFurthestPoints.getFirst();
      var p2 = mainFurthestPoints.getLast();

      assert p1 != p2;

      var p3Candidates = perpendicular.getFurthestPoints();
      var p3 = p3Candidates.getFirst();

      if (p1 == p3 || p2 == p3) {
        p3 = p3Candidates.getLast();
      }

      assert p3 != p1 && p3 != p2;
      return List.of(p1, p2, p3);
    }

    public List<LasPointGeometry> getPoints() {
      var points = new ArrayList<>(main.getPoints());
      points.addAll(perpendicular.getPoints());
      return points;
    }
  }

  @Builder
  public record Conf(
      int attempts,
      int maxLength,
      double degEpsilon,
      double minVectorNorm,
      double squaredThreshold) {}
}
