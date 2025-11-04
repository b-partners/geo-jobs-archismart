package app.bpartners.geojobs.endpoint.rest.readme.monitor;

import static java.time.Instant.now;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.readme.ReadmeLogCreated;
import app.bpartners.geojobs.endpoint.rest.readme.monitor.factory.ReadmeLogFactory;
import app.bpartners.geojobs.endpoint.rest.security.AuthProvider;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
@RequiredArgsConstructor
public class ReadmeMonitorFilter extends OncePerRequestFilter {
  private final RequestMatcher requestMatcher;
  private final ReadmeMonitorConf readmeMonitorConf;
  private final ReadmeLogFactory readmeLogFactory;
  private final EventProducer eventProducer;
  private final AuthProvider authProvider;

  @Override
  @SneakyThrows
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) {
    if (!requestMatcher.matches(request)) {
      filterChain.doFilter(request, response);
      return;
    }

    var startedDatetime = now();
    filterChain.doFilter(request, response);

    try {
      var readmeLog =
          readmeLogFactory.createReadmeLog(
              request,
              response,
              startedDatetime,
              now(),
              authProvider.getPrincipal(),
              readmeMonitorConf);
      eventProducer.accept(
          List.of(
              ReadmeLogCreated.builder()
                  .readmeMonitorConf(readmeMonitorConf)
                  .readmeLog(readmeLog)
                  .build()));
    } catch (Exception e) {
      log.error("Cannot create readmeLog: {}", e.getMessage());
    }
  }
}
