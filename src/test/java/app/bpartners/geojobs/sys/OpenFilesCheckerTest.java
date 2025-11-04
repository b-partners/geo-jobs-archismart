package app.bpartners.geojobs.sys;

import static java.lang.Thread.sleep;

import org.junit.jupiter.api.Test;

class OpenFilesCheckerTest {
  @Test
  void start_and_stop() throws InterruptedException {
    var subject = new OpenFilesChecker();

    subject.start();
    sleep(15_000);
    subject.stop();
  }
}
