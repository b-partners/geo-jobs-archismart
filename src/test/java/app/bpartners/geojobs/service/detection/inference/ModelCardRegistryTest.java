package app.bpartners.geojobs.service.detection.inference;

import static app.bpartners.geojobs.service.detection.inference.DeployedModelCard.ProblemType.OBJECT_DETECTION;
import static app.bpartners.geojobs.service.detection.inference.DeployedModelCard.ProblemType.SEMANTIC_SEGMENTATION;
import static java.time.Duration.ofHours;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;

class ModelCardRegistryTest {
  private static final DeployedModelCard DAMAGES =
      new DeployedModelCard("damages", SEMANTIC_SEGMENTATION, 1, true, 1024, 1024, true);
  private static final DeployedModelCard TOMBS =
      new DeployedModelCard("tombs", OBJECT_DETECTION, 1, false, null, null, null);
  private static final DeployedModelCard NOT_RUNNABLE =
      new DeployedModelCard("mutation", SEMANTIC_SEGMENTATION, 2, true, 1024, 1024, false);

  InferenceApiClient client = mock();
  MutableClock clock = new MutableClock(Instant.parse("2026-10-07T00:00:00Z"));
  ModelCardRegistry subject = new ModelCardRegistry(client, ofHours(8), clock);

  @Test
  void keeps_only_runnable_model_cards() {
    when(client.getModelCards()).thenReturn(List.of(DAMAGES, TOMBS, NOT_RUNNABLE));

    assertEquals(DAMAGES, subject.findById("damages").orElseThrow());
    assertEquals(TOMBS, subject.findById("tombs").orElseThrow());
    assertTrue(subject.findById("mutation").isEmpty());
    assertTrue(subject.findById("unknown").isEmpty());
    assertEquals(2, subject.getRunnableModelCards().size());
  }

  @Test
  void calls_api_once_within_ttl() {
    when(client.getModelCards()).thenReturn(List.of(DAMAGES));

    subject.findById("damages");
    clock.advance(ofHours(7));
    subject.findById("damages");
    subject.getRunnableModelCards();

    verify(client, times(1)).getModelCards();
  }

  @Test
  void refreshes_after_ttl() {
    when(client.getModelCards()).thenReturn(List.of(DAMAGES)).thenReturn(List.of(TOMBS));

    assertTrue(subject.findById("damages").isPresent());
    clock.advance(ofHours(8));

    assertTrue(subject.findById("damages").isEmpty());
    assertTrue(subject.findById("tombs").isPresent());
    verify(client, times(2)).getModelCards();
  }

  @Test
  void serves_stale_model_cards_when_refresh_fails() {
    when(client.getModelCards())
        .thenReturn(List.of(DAMAGES))
        .thenThrow(new IllegalStateException("down"));

    subject.findById("damages");
    clock.advance(ofHours(9));

    assertTrue(subject.findById("damages").isPresent());
  }

  @Test
  void throws_when_first_load_fails() {
    when(client.getModelCards()).thenThrow(new IllegalStateException("down"));

    assertThrows(IllegalStateException.class, () -> subject.findById("damages"));
  }

  @Test
  void invalidate_forces_reload() {
    when(client.getModelCards()).thenReturn(List.of(DAMAGES));

    subject.findById("damages");
    subject.invalidate();
    subject.findById("damages");

    verify(client, times(2)).getModelCards();
  }

  private static class MutableClock extends Clock {
    private Instant now;

    MutableClock(Instant now) {
      this.now = now;
    }

    void advance(java.time.Duration duration) {
      now = now.plus(duration);
    }

    @Override
    public java.time.ZoneId getZone() {
      return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(java.time.ZoneId zone) {
      return this;
    }

    @Override
    public Instant instant() {
      return now;
    }
  }
}
