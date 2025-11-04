package app.bpartners.geojobs.endpoint.rest.readme.monitor.factory;

import static app.bpartners.geojobs.endpoint.rest.readme.monitor.model.ReadmeRequest.ReadmeRequestLog;

import app.bpartners.geojobs.endpoint.rest.readme.monitor.ReadmeMonitorConf;
import app.bpartners.geojobs.endpoint.rest.readme.monitor.model.ReadmeLog;
import app.bpartners.geojobs.endpoint.rest.readme.monitor.model.ReadmeRequest;
import app.bpartners.geojobs.endpoint.rest.security.model.Principal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReadmeLogFactory {
  private final ReadmeGroupFactory readmeGroupFactory;
  private final ReadmeEntryFactory readmeEntryFactory;
  private final ReadmeRequestCreatorFactory readmeRequestCreatorFactory;

  public ReadmeLog createReadmeLog(
      HttpServletRequest request,
      HttpServletResponse response,
      Instant requestStartedDatetime,
      Instant requestEndedDatetime,
      Principal principal,
      ReadmeMonitorConf readmeMonitorConf) {
    return ReadmeLog.builder()
        .clientIPAddress(request.getRemoteAddr())
        .development(readmeMonitorConf.isDevelopment())
        .group(readmeGroupFactory.createReadmeGroup(principal))
        .request(
            ReadmeRequest.builder()
                .log(
                    ReadmeRequestLog.builder()
                        .creator(
                            readmeRequestCreatorFactory.createReadmeRequestCreator(
                                readmeMonitorConf))
                        .entries(
                            List.of(
                                readmeEntryFactory.createReadmeEntry(
                                    request,
                                    response,
                                    requestStartedDatetime,
                                    requestEndedDatetime)))
                        .build())
                .build())
        .build();
  }
}
