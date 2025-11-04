package app.bpartners.geojobs.job.model;

import static app.bpartners.geojobs.job.model.Status.HealthStatus.UNKNOWN;
import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.PENDING;
import static java.time.Instant.now;
import static java.util.Comparator.comparing;
import static java.util.Comparator.naturalOrder;

import java.util.List;

public interface Statusable<S extends Status> {
  List<S> getStatusHistory();

  void setStatusHistory(List<S> statusHistory);

  default S getStatus() {
    var statusHistory = getStatusHistory();
    return statusHistory.isEmpty()
        ? from(
            Status.builder().progression(PENDING).health(UNKNOWN).creationDatetime(now()).build())
        : from(
            statusHistory.stream()
                .map(status -> (Status) status)
                .sorted(comparing(Status::getCreationDatetime, naturalOrder()).reversed())
                .toList()
                .getFirst());
  }

  default Statusable<S> hasNewStatus(Status status) {
    var statusHistory = getStatusHistory();
    var subtypedStatus = from(status);
    if (statusHistory.isEmpty()) {
      statusHistory.add(subtypedStatus);
    } else {
      statusHistory.add(from(getStatus().to(subtypedStatus)));
    }
    return this;
  }

  S from(Status status);
}
