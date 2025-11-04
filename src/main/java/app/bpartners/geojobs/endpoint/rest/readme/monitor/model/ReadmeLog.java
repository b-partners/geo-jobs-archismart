package app.bpartners.geojobs.endpoint.rest.readme.monitor.model;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.io.Serializable;
import lombok.Builder;

/*
 *   docs: https://docs.readme.com/main/reference/post_request
 * */
@Builder
@JsonInclude(NON_NULL)
public record ReadmeLog(
    String clientIPAddress, Boolean development, ReadmeGroup group, ReadmeRequest request)
    implements Serializable {}
