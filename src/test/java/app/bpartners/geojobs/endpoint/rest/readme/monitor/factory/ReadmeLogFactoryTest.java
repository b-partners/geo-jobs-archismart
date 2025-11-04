package app.bpartners.geojobs.endpoint.rest.readme.monitor.factory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import app.bpartners.geojobs.endpoint.rest.readme.monitor.ReadmeMonitorConf;
import app.bpartners.geojobs.endpoint.rest.readme.monitor.model.ReadmeGroup;
import app.bpartners.geojobs.endpoint.rest.readme.monitor.model.ReadmeLog;
import app.bpartners.geojobs.endpoint.rest.readme.monitor.model.ReadmeRequest;
import app.bpartners.geojobs.endpoint.rest.readme.monitor.model.ReadmeRequestCreator;
import app.bpartners.geojobs.endpoint.rest.readme.monitor.model.entry.ReadmeEntry;
import app.bpartners.geojobs.endpoint.rest.security.model.Principal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class ReadmeLogFactoryTest {
  private static final String REMOTE_ADDR = "192.162.15.1";
  ReadmeGroupFactory readmeGroupFactoryMock = mock();
  ReadmeEntryFactory readmeEntryFactoryMock = mock();
  HttpServletRequest httpServletRequestMock = mock();
  HttpServletResponse httpServletResponseMock = mock();
  ReadmeLogFactory subject =
      new ReadmeLogFactory(
          readmeGroupFactoryMock, readmeEntryFactoryMock, new ReadmeRequestCreatorFactory());

  @Test
  void can_create_readme_log() {
    var readmeGroupMock = mock(ReadmeGroup.class);
    var readmeEntryMock = mock(ReadmeEntry.class);
    var readmeMonitorConf = readmeMonitorConf();
    when(readmeGroupFactoryMock.createReadmeGroup(any())).thenReturn(readmeGroupMock);
    when(readmeEntryFactoryMock.createReadmeEntry(any(), any(), any(), any()))
        .thenReturn(readmeEntryMock);
    when(httpServletRequestMock.getRemoteAddr()).thenReturn(REMOTE_ADDR);

    var expected = expectedReadmeLog(readmeGroupMock, readmeEntryMock, readmeMonitorConf);

    var actual =
        subject.createReadmeLog(
            httpServletRequestMock,
            httpServletResponseMock,
            mock(Instant.class),
            mock(Instant.class),
            mock(Principal.class),
            readmeMonitorConf);

    assertEquals(expected, actual);
  }

  private ReadmeLog expectedReadmeLog(
      ReadmeGroup readmeGroup, ReadmeEntry readmeEntry, ReadmeMonitorConf conf) {
    return ReadmeLog.builder()
        .group(readmeGroup)
        .request(
            ReadmeRequest.builder()
                .log(
                    ReadmeRequest.ReadmeRequestLog.builder()
                        .creator(
                            ReadmeRequestCreator.builder()
                                .name(conf.getName())
                                .version(conf.getVersion())
                                .build())
                        .entries(List.of(readmeEntry))
                        .build())
                .build())
        .clientIPAddress(REMOTE_ADDR)
        .development(conf.isDevelopment())
        .build();
  }

  private ReadmeMonitorConf readmeMonitorConf() {
    return ReadmeMonitorConf.builder()
        .name("readmeio")
        .version("0.0.1")
        .url("https://dummy.com")
        .apiKey("readme-admin-api")
        .development(true)
        .build();
  }
}
