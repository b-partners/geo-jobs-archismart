package app.bpartners.geojobs.service.tiling;

import static app.bpartners.geojobs.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;
import static java.lang.Runtime.getRuntime;
import static java.util.concurrent.Executors.newFixedThreadPool;
import static java.util.stream.Collectors.toSet;

import app.bpartners.geojobs.model.exception.ApiException;
import app.bpartners.geojobs.repository.model.tiling.ParcelTilingTask;
import app.bpartners.geojobs.repository.model.tiling.ZoneTilingJob;
import app.bpartners.geojobs.service.event.TilingTaskConsumer;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.function.BiFunction;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class RooferMadeTilingService
    implements BiFunction<ZoneTilingJob, List<ParcelTilingTask>, ZoneTilingJob> {
  private final TilingTaskConsumer tilingTaskConsumer;
  private final ZoneTilingJobService zoneTilingJobService;
  private final ExecutorService executorService =
      newFixedThreadPool(Math.max(1, getRuntime().availableProcessors() - 1));

  @Override
  public ZoneTilingJob apply(ZoneTilingJob job, List<ParcelTilingTask> tilingTasks) {
    try {
      executorService.invokeAll(
          tilingTasks.stream()
              .map(
                  task ->
                      ((Callable<ParcelTilingTask>)
                          () -> {
                            tilingTaskConsumer.accept(task);
                            return task;
                          }))
              .collect(toSet()));
      return zoneTilingJobService.create(job, tilingTasks);
    } catch (InterruptedException e) {
      throw new ApiException(SERVER_EXCEPTION, e.getMessage());
    }
  }
}
