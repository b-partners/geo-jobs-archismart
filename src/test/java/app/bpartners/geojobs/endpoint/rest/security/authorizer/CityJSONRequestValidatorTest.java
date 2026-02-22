package app.bpartners.geojobs.endpoint.rest.security.authorizer;

import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import app.bpartners.geojobs.model.exception.BadRequestException;
import app.bpartners.geojobs.repository.CityJSONRequestRepository;
import app.bpartners.geojobs.repository.model.cityjson.CityJSONRequest;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class CityJSONRequestValidatorTest {
  CityJSONRequestRepository cityJSONRequestRepositoryMock = mock(CityJSONRequestRepository.class);
  CityJSONRequestValidator subject = new CityJSONRequestValidator(cityJSONRequestRepositoryMock);

  @Test
  void do_nothing_when_not_exisint_request() {
    var requestId = randomUUID().toString();
    var communityOwnerId = randomUUID().toString();
    when(cityJSONRequestRepositoryMock.findById(requestId)).thenReturn(Optional.empty());

    assertDoesNotThrow(() -> subject.accept(requestId, communityOwnerId));
  }

  @Test
  void throws_error_when_request_already_exist() {
    var requestId = randomUUID().toString();
    var communityOwnerId = randomUUID().toString();
    when(cityJSONRequestRepositoryMock.findByIdAndCommunityOwnerId(requestId, communityOwnerId))
        .thenReturn(
            Optional.of(CityJSONRequest.builder().communityOwnerId(communityOwnerId).build()));

    var actual =
        assertThrows(BadRequestException.class, () -> subject.accept(requestId, communityOwnerId));

    assertEquals(
        "Process request with id " + requestId + " can not be either updated or processed again",
        actual.getMessage());
  }
}
