package app.bpartners.geojobs.endpoint.rest.readme.monitor.model;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

import app.bpartners.geojobs.endpoint.rest.readme.monitor.model.entry.ReadmeEntry;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.io.Serializable;
import java.util.List;
import lombok.Builder;

@Builder
@JsonInclude(NON_NULL)
public record ReadmeRequest(ReadmeRequestLog log) implements Serializable {

  @Builder
  @JsonInclude(NON_NULL)
  public record ReadmeRequestLog(ReadmeRequestCreator creator, List<ReadmeEntry> entries)
      implements Serializable {}
}
