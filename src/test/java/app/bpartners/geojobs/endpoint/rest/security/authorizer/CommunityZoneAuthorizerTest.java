package app.bpartners.geojobs.endpoint.rest.security.authorizer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import app.bpartners.geojobs.endpoint.rest.controller.v1.mapper.FeatureMapper;
import app.bpartners.geojobs.endpoint.rest.model.Feature;
import app.bpartners.geojobs.endpoint.rest.model.FeatureGeometry;
import app.bpartners.geojobs.endpoint.rest.model.MultiPolygon;
import app.bpartners.geojobs.endpoint.rest.security.model.Principal;
import app.bpartners.geojobs.model.exception.ForbiddenException;
import app.bpartners.geojobs.repository.model.community.CommunityAuthorization;
import app.bpartners.geojobs.repository.model.community.CommunityAuthorizedZone;
import app.bpartners.geojobs.service.geojson.GeometryConverter;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CommunityZoneAuthorizerTest {
  FeatureMapper featureMapper = new FeatureMapper(new GeometryConverter(), null);
  CommunityZoneAuthorizer subject = new CommunityZoneAuthorizer(featureMapper);
  Principal principalMock = mock(Principal.class);

  @BeforeEach
  void setUp() {
    when(principalMock.isAdmin()).thenReturn(false);
  }

  @Test
  void do_nothing_when_admin() {
    var principal = mock(Principal.class);
    when(principal.isAdmin()).thenReturn(true);

    assertDoesNotThrow(() -> subject.accept(new CommunityAuthorization(), List.of(), principal));
    verify(principal, only()).isAdmin();
  }

  @Test
  void do_nothing_when_insurance() {
    var principal = mock(Principal.class);
    when(principal.isInsurance()).thenReturn(true);

    assertDoesNotThrow(() -> subject.accept(new CommunityAuthorization(), List.of(), principal));
    verify(principal, times(1)).isInsurance();
  }

  @Test
  void should_throws_if_community_does_not_have_authorized_zone() {
    List<Feature> features = List.of();
    var communityAuthorization = communityAuthorization();
    communityAuthorization.setAuthorizedZones(List.of());

    var error =
        assertThrows(
            ForbiddenException.class,
            () -> {
              subject.accept(communityAuthorization, features, principalMock);
            });
    assertTrue(error.getMessage().contains("There is no zone authorized"));
  }

  @Test
  void should_throws_if_feature_is_not_covered_by_authorized_zone() {
    var features = List.of(feature50SurfaceOutside2000());
    var communityAuthorization = communityAuthorization();

    var error =
        assertThrows(
            ForbiddenException.class,
            () -> {
              subject.accept(communityAuthorization, features, principalMock);
            });
    assertTrue(error.getMessage().contains("Some given feature is not allowed"));
  }

  @Test
  void should_accept_if_feature_are_covered_by_authorized_zone() {
    var features = List.of(feature1550InsideSurface());
    var communityAuthorization = communityAuthorization();

    assertDoesNotThrow(
        () -> {
          subject.accept(communityAuthorization, features, principalMock);
        });
  }

  private CommunityAuthorizedZone communityAuthorizedZone() {
    return CommunityAuthorizedZone.builder()
        .multiPolygon(feature2000Surface().getGeometry().getMultiPolygon())
        .build();
  }

  private CommunityAuthorization communityAuthorization() {
    return CommunityAuthorization.builder()
        .authorizedZones(List.of(communityAuthorizedZone()))
        .build();
  }

  private Feature feature1550InsideSurface() {
    Feature feature = new Feature();
    BigDecimal sideLength = BigDecimal.valueOf(Math.sqrt(1550));
    var coordinates =
        List.of(
            List.of(
                List.of(BigDecimal.valueOf(2.675), BigDecimal.valueOf(2.675)),
                List.of(BigDecimal.valueOf(2.675), sideLength.add(BigDecimal.valueOf(2.675))),
                List.of(
                    sideLength.add(BigDecimal.valueOf(2.675)),
                    sideLength.add(BigDecimal.valueOf(2.675))),
                List.of(sideLength.add(BigDecimal.valueOf(2.675)), BigDecimal.valueOf(2.675)),
                List.of(BigDecimal.valueOf(2.675), BigDecimal.valueOf(2.675))));
    MultiPolygon multiPolygon = new MultiPolygon().coordinates(List.of(coordinates));
    feature.setGeometry(new FeatureGeometry(multiPolygon));
    return feature;
  }

  private Feature feature50SurfaceOutside2000() {
    Feature feature = new Feature();
    var sideLength = Math.sqrt(50);
    var coordinates =
        List.of(
            List.of(
                List.of(BigDecimal.valueOf(40), BigDecimal.valueOf(-5)),
                List.of(BigDecimal.valueOf(40), BigDecimal.valueOf(-5 + sideLength)),
                List.of(BigDecimal.valueOf(40 + sideLength), BigDecimal.valueOf(-5 + sideLength)),
                List.of(BigDecimal.valueOf(40 + sideLength), BigDecimal.valueOf(-5)),
                List.of(BigDecimal.valueOf(40), BigDecimal.valueOf(-5))));
    MultiPolygon multiPolygon = new MultiPolygon().coordinates(List.of(coordinates));
    feature.setGeometry(new FeatureGeometry(multiPolygon));
    return feature;
  }

  private Feature feature2000Surface() {
    Feature feature = new Feature();
    var coordinates =
        List.of(
            List.of(
                List.of(BigDecimal.valueOf(0), BigDecimal.valueOf(0)),
                List.of(BigDecimal.valueOf(0), BigDecimal.valueOf(44.72)),
                List.of(BigDecimal.valueOf(44.72), BigDecimal.valueOf(44.72)),
                List.of(BigDecimal.valueOf(44.72), BigDecimal.valueOf(0)),
                List.of(BigDecimal.valueOf(0), BigDecimal.valueOf(0))));
    MultiPolygon multiPolygon = new MultiPolygon().coordinates(List.of(coordinates));
    feature.setGeometry(new FeatureGeometry(multiPolygon));
    return feature;
  }
}
