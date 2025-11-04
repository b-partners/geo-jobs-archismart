package app.bpartners.geojobs.sqs;

import static java.util.concurrent.TimeUnit.SECONDS;

import app.bpartners.geojobs.endpoint.event.consumer.EventConsumer;
import app.bpartners.geojobs.endpoint.event.consumer.model.ConsumableEvent;
import app.bpartners.geojobs.endpoint.event.model.PojaEvent;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Scope("prototype")
public class LocalEventQueue {
  private static final int DEFAULT_MAX_ATTEMPT = 8;
  private static final int CUSTOM_MAX_ATTEMPT = 5;
  private static final int MAX_SHUTDOWN_ATTEMPT = 3;
  final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
  private List<ConsumableEvent> remainingEvents = new ArrayList<>();
  private List<ConsumableEvent> succeededEvents = new ArrayList<>();
  private List<ConsumableEvent> dlQueue = new ArrayList<>();
  private int eventNb;
  private int defaultDelayExp = 1;
  private int shutDownAttempt = 0;
  List<CustomEventDelayConfig> customEventConfigList = new ArrayList<>();
  @Autowired private EventConsumer eventConsumer;

  public void configure(List<CustomEventDelayConfig> customEventConfigList, int defaultDelayExp) {
    this.defaultDelayExp = defaultDelayExp;
    this.customEventConfigList = customEventConfigList;
  }

  public void attemptSchedulerShutDown() {
    int remainingEventSize = remainingEvents.size();
    if (allEventsHandled()) {
      log.info(
          "Local SQS Queue : totalEvents={}, succeeded(size={},value={}), failed(size={},"
              + " value={})",
          eventNb,
          succeededEvents.size(),
          succeededEvents,
          dlQueue.size(),
          dlQueue);
      scheduler.shutdown();
    } else {
      log.info(
          "Remaining events in the queue (size={}, value={}",
          remainingEventSize,
          remainingEvents.stream()
              .filter(Objects::nonNull)
              .map(event -> event.getEvent().typeName())
              .toList());
      if (shutDownAttempt < MAX_SHUTDOWN_ATTEMPT) {
        shutDownAttempt++;
        scheduler.schedule(this::attemptSchedulerShutDown, 15L, SECONDS);
      } else {
        log.info("Failed to handle all remaining events {}", remainingEventSize);
        scheduler.shutdownNow();
      }
    }
  }

  private boolean allEventsHandled() {
    return remainingEvents.isEmpty() && (succeededEvents.size() + dlQueue.size() == eventNb);
  }

  public void handle(List<ConsumableEvent> consumableEvents) {
    consumableEvents.forEach(this::handle);
  }

  private void handle(ConsumableEvent consumableEvent) {
    var pojaEvent = consumableEvent.getEvent().payload();
    int attemptNb = getAttemptNb(consumableEvent) + 1;
    log.info("Attempt nb : {}, Event : {}", attemptNb, pojaEvent);
    if ((isCustomEvent(pojaEvent) && attemptNb <= DEFAULT_MAX_ATTEMPT)
        || (!isCustomEvent(pojaEvent) && attemptNb <= CUSTOM_MAX_ATTEMPT)) {
      try {
        add(consumableEvent);
        eventConsumer.accept(List.of(consumableEvent));
      } catch (Exception e) {
        log.info("Exception occurs when handling {} : {}", pojaEvent, e.getMessage());
        long defaultDelay = pojaEvent.maxConsumerBackoffBetweenRetries().getSeconds();
        long actualDelay =
            verifyPojaCustomEvent(pojaEvent)
                .map(config -> defaultDelay / config.delayExp)
                .orElse(defaultDelay / defaultDelayExp);
        scheduler.schedule(() -> handle(consumableEvent), actualDelay, SECONDS);
        return;
      }
      succeed(consumableEvent);
    } else {
      fail(consumableEvent);
    }
  }

  @NonNull
  private Optional<CustomEventDelayConfig> verifyPojaCustomEvent(PojaEvent pojaEvent) {
    return customEventConfigList.stream()
        .filter(config -> config.pojaEventClass.equals(pojaEvent.getClass()))
        .findFirst();
  }

  private boolean isCustomEvent(PojaEvent pojaEvent) {
    return verifyPojaCustomEvent(pojaEvent).isPresent();
  }

  private void add(ConsumableEvent event) {
    if (remainingEvents == null) {
      remainingEvents = new ArrayList<>();
      remainingEvents.add(event);
      eventNb++;
    } else {
      if (!remainingEvents.contains(event)) {
        remainingEvents.add(event);
        eventNb++;
      } else {
        var existingEvent =
            remainingEvents.stream().filter(e -> e.equals(event)).findAny().orElseThrow();
        setAttemptNb(existingEvent, getAttemptNb(existingEvent) + 1);
      }
    }
  }

  private void succeed(ConsumableEvent event) {
    if (succeededEvents == null) succeededEvents = new ArrayList<>();
    if (!remainingEvents.contains(event)) {
      throw new NoSuchElementException(event + " not found");
    }
    remainingEvents.remove(event);
    succeededEvents.add(event);
  }

  private void fail(ConsumableEvent event) {
    if (dlQueue == null) dlQueue = new ArrayList<>();
    if (!remainingEvents.contains(event)) {
      throw new NoSuchElementException(event + " not found");
    }
    remainingEvents.remove(event);
    dlQueue.add(event);
  }

  private int getAttemptNb(ConsumableEvent event) {
    return event.getEvent().payload().getAttemptNb();
  }

  private void setAttemptNb(ConsumableEvent event, int attemptNb) {
    event.getEvent().payload().setAttemptNb(attemptNb);
  }

  /**
   * This class is used for configuring custom delay for specific given PojaEvent Only provided
   * pojaEventClass have backOff override Its usage is :
   * pojaEvent.maxConsumerBackoffBetweenRetries().getSeconds() / delayExp
   */
  public record CustomEventDelayConfig(Class<? extends PojaEvent> pojaEventClass, int delayExp) {}
}
