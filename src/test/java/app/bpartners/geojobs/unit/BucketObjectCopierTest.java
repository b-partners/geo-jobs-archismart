package app.bpartners.geojobs.unit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.bpartners.geojobs.file.bucket.BucketConf;
import app.bpartners.geojobs.file.bucket.BucketObjectCopier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;

class BucketObjectCopierTest {
  BucketConf bucketConf = mock();
  S3Client s3Client = mock();
  BucketObjectCopier subject = new BucketObjectCopier(bucketConf);

  @BeforeEach
  void setUp() {
    when(bucketConf.getBucketName()).thenReturn("bucket");
    when(bucketConf.getS3Client()).thenReturn(s3Client);
  }

  @Test
  void copies_the_object_inside_the_same_bucket() {
    subject.copy("zip/source/result.zip", "zip/target/result.zip");

    var captor = ArgumentCaptor.forClass(CopyObjectRequest.class);
    verify(s3Client).copyObject(captor.capture());
    var request = captor.getValue();
    assertEquals("bucket", request.sourceBucket());
    assertEquals("zip/source/result.zip", request.sourceKey());
    assertEquals("bucket", request.destinationBucket());
    assertEquals("zip/target/result.zip", request.destinationKey());
  }

  @Test
  void deletes_the_object() {
    subject.deleteQuietly("zip/target/result.zip");

    var captor = ArgumentCaptor.forClass(DeleteObjectRequest.class);
    verify(s3Client).deleteObject(captor.capture());
    assertEquals("zip/target/result.zip", captor.getValue().key());
  }

  @Test
  void deletion_failure_is_swallowed() {
    when(s3Client.deleteObject(any(DeleteObjectRequest.class)))
        .thenThrow(new IllegalStateException("s3 down"));

    assertDoesNotThrow(() -> subject.deleteQuietly("zip/target/result.zip"));
  }
}
