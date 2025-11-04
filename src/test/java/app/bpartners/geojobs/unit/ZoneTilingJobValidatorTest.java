package app.bpartners.geojobs.unit;

import app.bpartners.geojobs.endpoint.rest.model.CreateZoneTilingJob;
import app.bpartners.geojobs.endpoint.rest.model.ImportZoneTilingJob;
import app.bpartners.geojobs.endpoint.rest.validator.ZoneTilingJobValidator;
import app.bpartners.geojobs.model.exception.BadRequestException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ZoneTilingJobValidatorTest {
  ZoneTilingJobValidator subject = new ZoneTilingJobValidator();

  @Test
  void accept_ok() {
    Assertions.assertDoesNotThrow(
        () ->
            subject.accept(
                new ImportZoneTilingJob()
                    .createZoneTilingJob(new CreateZoneTilingJob())
                    .bucketName("dummy")));
  }

  @Test
  void accept_ko() {
    Assertions.assertThrows(
        BadRequestException.class,
        () ->
            subject.accept(
                new ImportZoneTilingJob().createZoneTilingJob(null).bucketName("dummy")));
    Assertions.assertThrows(
        BadRequestException.class,
        () -> subject.accept(new ImportZoneTilingJob().createZoneTilingJob(null).bucketName(null)));
    Assertions.assertThrows(
        BadRequestException.class,
        () ->
            subject.accept(
                new ImportZoneTilingJob()
                    .createZoneTilingJob(new CreateZoneTilingJob())
                    .bucketName(null)));
  }
}
