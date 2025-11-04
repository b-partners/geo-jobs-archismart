package app.bpartners.geojobs.utils;

import app.bpartners.geojobs.service.event.AnnotationRetriever;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import org.slf4j.LoggerFactory;

public class LogCaptor extends AppenderBase<ILoggingEvent> {
  private static Logger logger = (Logger) LoggerFactory.getLogger(AnnotationRetriever.class);
  @Getter private final List<ILoggingEvent> logEvents;

  public LogCaptor() {
    this.logEvents = new ArrayList<>();
  }

  @Override
  protected void append(ILoggingEvent eventObject) {
    logEvents.add(eventObject);
  }

  public void configure() {
    this.setContext(logger.getLoggerContext());
    logger.addAppender(this);
    this.start();
  }
}
