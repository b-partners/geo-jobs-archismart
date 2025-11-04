package app.bpartners.geojobs.endpoint.rest.readme.monitor.model.entry;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.io.Serializable;
import java.util.List;
import lombok.Builder;

@Builder
@JsonInclude(NON_NULL)
public record ReadmeEntryRequest(
    String method,
    String url,
    String httpVersion,
    List<ReadmeEntryHeader> headers,
    List<ReadmeEntryQuery> queryString)
    implements Serializable {}
