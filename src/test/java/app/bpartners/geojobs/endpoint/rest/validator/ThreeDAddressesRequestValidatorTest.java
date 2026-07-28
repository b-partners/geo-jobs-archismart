package app.bpartners.geojobs.endpoint.rest.validator;

import static app.bpartners.geojobs.endpoint.rest.model.DelimitationType.PARCEL_CONSTRAINED_DELIMITATION;
import static app.bpartners.geojobs.endpoint.rest.model.DelimitationType.PARCEL_FREE_DELIMITATION;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.*;

import app.bpartners.geojobs.endpoint.rest.model.AddressFullText;
import app.bpartners.geojobs.endpoint.rest.model.ThreeDAddressesRequest;
import app.bpartners.geojobs.model.exception.BadRequestException;
import app.bpartners.geojobs.model.exception.NotImplementedException;
import app.bpartners.geojobs.validator.ThreeDAddressesRequestValidator;
import java.util.List;
import org.junit.jupiter.api.Test;

class ThreeDAddressesRequestValidatorTest {
  ThreeDAddressesRequestValidator subject = new ThreeDAddressesRequestValidator();

  @Test
  void throws_bad_request_exception_when_no_addresses() {
    var actualExceptionFromNull =
        assertThrows(BadRequestException.class, () -> subject.accept(null));
    var actualExceptionFromNullAddresses =
        assertThrows(
            BadRequestException.class,
            () -> subject.accept(new ThreeDAddressesRequest().addresses(null)));
    var actualExceptionFromEmptyAddresses =
        assertThrows(
            BadRequestException.class,
            () -> subject.accept(new ThreeDAddressesRequest().addresses(List.of())));
    var actualExceptionFromNullAddressValue =
        assertThrows(
            BadRequestException.class,
            () ->
                subject.accept(
                    new ThreeDAddressesRequest()
                        .addresses(List.of(new AddressFullText().fullText(null)))));
    var actualExceptionFromEmptyStringAddressValue =
        assertThrows(
            BadRequestException.class,
            () ->
                subject.accept(
                    new ThreeDAddressesRequest()
                        .addresses(List.of(new AddressFullText().fullText("")))));

    assertEquals("Addresses can not be null or empty", actualExceptionFromNull.getMessage());
    assertEquals(
        "Addresses can not be null or empty", actualExceptionFromNullAddresses.getMessage());
    assertEquals(
        "Addresses can not be null or empty", actualExceptionFromEmptyAddresses.getMessage());
    assertEquals(
        "Addresses can not be null or empty", actualExceptionFromNullAddressValue.getMessage());
    assertEquals(
        "Addresses can not be null or empty",
        actualExceptionFromEmptyStringAddressValue.getMessage());
  }

  @Test
  void throws_not_implemented_exception_when_delimitation_type_not_PARCEL_FREE_DELIMITATION() {
    var actualNotImplementedException =
        assertThrows(
            NotImplementedException.class,
            () ->
                subject.accept(
                    new ThreeDAddressesRequest()
                        .addresses(List.of(new AddressFullText().fullText(randomUUID().toString())))
                        .delimitationType(PARCEL_CONSTRAINED_DELIMITATION)));

    assertEquals(
        "Only PARCEL_FREE_DELIMITATION is supported to request 3D model on addresses for now but"
            + " actual is "
            + PARCEL_CONSTRAINED_DELIMITATION,
        actualNotImplementedException.getMessage());
  }

  @Test
  void do_nothing_when_all_required_args_provided_or_correct() {
    assertDoesNotThrow(
        () ->
            subject.accept(
                new ThreeDAddressesRequest()
                    .addresses(List.of(new AddressFullText().fullText(randomUUID().toString())))
                    .delimitationType(PARCEL_FREE_DELIMITATION)));

    assertDoesNotThrow(
        () ->
            subject.accept(
                new ThreeDAddressesRequest()
                    .addresses(List.of(new AddressFullText().fullText(randomUUID().toString())))
                    .delimitationType(null)));
  }
}
