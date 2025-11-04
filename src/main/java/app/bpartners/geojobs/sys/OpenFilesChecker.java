package app.bpartners.geojobs.sys;

import static java.lang.management.ManagementFactory.getOperatingSystemMXBean;
import static java.util.concurrent.Executors.newScheduledThreadPool;
import static java.util.concurrent.TimeUnit.SECONDS;

import com.sun.management.UnixOperatingSystemMXBean;
import java.time.Duration;
import java.util.concurrent.ScheduledExecutorService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class OpenFilesChecker {
  private final UnixOperatingSystemMXBean os;

  private final ScheduledExecutorService scheduler = newScheduledThreadPool(1);
  private final Duration frequencyCheck = Duration.ofSeconds(5);

  public OpenFilesChecker() {
    this.os =
        getOperatingSystemMXBean() instanceof UnixOperatingSystemMXBean
            ? (UnixOperatingSystemMXBean) getOperatingSystemMXBean()
            : null;
    if (os == null) {
      throw new RuntimeException("Cannot check open files on non-unix systems");
    }
  }

  public void start() {
    os.getOpenFileDescriptorCount();
    log.info("Checking open files every: {}", frequencyCheck);
    scheduler.scheduleAtFixedRate(this::checkOpenFiles, 0, frequencyCheck.toSeconds(), SECONDS);
  }

  public void checkOpenFiles() {
    var ofCountByMxBean = os.getOpenFileDescriptorCount();
    var maxOfByMxBean = os.getMaxFileDescriptorCount();
    log.info("ofCountByMxBean={}, maxOfByMxBean={}", ofCountByMxBean, maxOfByMxBean);
  }

  public void stop() {
    scheduler.shutdown();
  }
}
