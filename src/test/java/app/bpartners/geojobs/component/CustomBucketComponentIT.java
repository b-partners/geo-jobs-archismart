package app.bpartners.geojobs.component;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.bpartners.geojobs.conf.FacadeIT;
import app.bpartners.geojobs.file.bucket.CustomBucketComponent;
import java.util.List;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import software.amazon.awssdk.services.s3.model.S3Object;

@Disabled(
    "TODO: must be launched only locally by configuration AWS credentials and EventConf.region")
public class CustomBucketComponentIT extends FacadeIT {
  @Autowired private CustomBucketComponent subject;

  @Test
  void list_objects_ok() {
    List<S3Object> actual = subject.listObjects("cannes-draft");

    assertEquals(6, actual.size());
    assertTrue(
        actual.stream()
            .anyMatch(s3Object -> s3Object.key().equals("draft_layer/20/544785/383260.jpg")));
    assertTrue(
        actual.stream()
            .anyMatch(s3Object -> s3Object.key().equals("draft_layer/20/544785/383261.jpg")));
    assertTrue(
        actual.stream()
            .anyMatch(s3Object -> s3Object.key().equals("draft_layer/20/544785/383262.jpg")));
    assertTrue(
        actual.stream()
            .anyMatch(s3Object -> s3Object.key().equals("draft_copy/20/544785/383260.jpg")));
    assertTrue(
        actual.stream()
            .anyMatch(s3Object -> s3Object.key().equals("draft_copy/20/544785/383261.jpg")));
    assertTrue(
        actual.stream()
            .anyMatch(s3Object -> s3Object.key().equals("draft_copy/20/544785/383262.jpg")));
  }

  @Test
  void list_objects_for_zones_ok() {
    List<S3Object> zoneP1 = subject.listObjects("cannes-qgis-tiles", "cannes_zone_p1");
    List<S3Object> zoneP2 = subject.listObjects("cannes-qgis-tiles", "cannes_zone_p2");
    List<S3Object> zoneP3 = subject.listObjects("cannes-qgis-tiles", "cannes_zone_p3");
    List<S3Object> zoneP4 = subject.listObjects("cannes-qgis-tiles", "cannes_zone_p4");
    List<S3Object> zoneP5 = subject.listObjects("cannes-qgis-tiles", "cannes_zone_p5");

    assertEquals(2160, zoneP1.size());
    assertEquals(1652, zoneP2.size());
    assertEquals(1026, zoneP3.size());
    assertEquals(10332, zoneP4.size());
    assertEquals(6336, zoneP5.size());
  }

  @Test
  void list_objects_with_prefix_ok() {
    List<S3Object> actual = subject.listObjects("cannes-draft", "draft_layer");
    List<S3Object> actual2 = subject.listObjects("cannes-draft", "draft_copy");
    List<S3Object> actualByName =
        subject.listObjects("cannes-qgis-tiles-pathway", "cannes_zone_all/20/Fusion_All");

    assertEquals(30607, actualByName.size());
    assertEquals(3, actual.size());
    assertEquals(3, actual2.size());
    assertTrue(
        actual.stream()
            .anyMatch(s3Object -> s3Object.key().equals("draft_layer/20/544785/383260.jpg")));
    assertTrue(
        actual.stream()
            .anyMatch(s3Object -> s3Object.key().equals("draft_layer/20/544785/383261.jpg")));
    assertTrue(
        actual.stream()
            .anyMatch(s3Object -> s3Object.key().equals("draft_layer/20/544785/383262.jpg")));
    assertTrue(
        actual2.stream()
            .anyMatch(s3Object -> s3Object.key().equals("draft_copy/20/544785/383260.jpg")));
    assertTrue(
        actual2.stream()
            .anyMatch(s3Object -> s3Object.key().equals("draft_copy/20/544785/383261.jpg")));
    assertTrue(
        actual2.stream()
            .anyMatch(s3Object -> s3Object.key().equals("draft_copy/20/544785/383262.jpg")));
  }

  @Test
  void list_objects_with_prefix_null_ok() {
    List<S3Object> actual = subject.listObjects("lyon-toiture-revetement-50-images", null);

    assertTrue(actual.stream().allMatch(s3Object -> s3Object.key().contains("Lyon-Toiture/20")));
    assertEquals(50, actual.size());
  }
}
