package app.bpartners.geojobs.postprocessing.continuer;

import static java.lang.Runtime.getRuntime;
import static java.util.concurrent.Executors.newFixedThreadPool;
import static java.util.stream.Collectors.toSet;

import app.bpartners.geojobs.model.geometry.IntXY;
import app.bpartners.geojobs.model.geometry.route.RoutesContinuationConf;
import app.bpartners.geojobs.postprocessing.NeighbourHoodHandler;
import app.bpartners.geojobs.postprocessing.model.TiledPolygon;
import app.bpartners.geojobs.postprocessing.model.TilingConf;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.stream.Stream;
import lombok.Getter;

public final class ParallelTiledLinesContinuer extends LinesContinuer<TiledPolygon> {
  @Getter private final TiledLinesContinuer tiledLinesContinuer;

  private final NeighbourHoodHandler neighbourHoodHandler;

  private final ExecutorService executorService =
      newFixedThreadPool(Math.max(1, getRuntime().availableProcessors() / 2));

  public ParallelTiledLinesContinuer(
      RoutesContinuationConf continuationConf,
      TilingConf tilingConf,
      int neighbourhoodTileDistance) {
    this.tiledLinesContinuer = new TiledLinesContinuer(continuationConf, tilingConf);
    this.neighbourHoodHandler = new NeighbourHoodHandler(neighbourhoodTileDistance);
  }

  @Override
  public Set<TiledPolygon> apply(Set<TiledPolygon> polygons) {
    Map<IntXY, Set<TiledPolygon>> polygonsByNeighbourhood = neighbourHoodHandler.apply(polygons);

    List<Future<Set<TiledPolygon>>> futures;
    try {
      futures =
          executorService.invokeAll(
              polygonsByNeighbourhood.values().stream()
                  .map(
                      neighbourhoodPolygons ->
                          ((Callable<Set<TiledPolygon>>)
                              () -> tiledLinesContinuer.apply(neighbourhoodPolygons)))
                  .collect(toSet()));
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }

    return futures.stream().flatMap(this::futureStream).collect(toSet());
  }

  private Stream<TiledPolygon> futureStream(Future<Set<TiledPolygon>> future) {
    try {
      return future.get().stream();
    } catch (InterruptedException | ExecutionException e) {
      throw new RuntimeException(e);
    }
  }
}
