package app.bpartners.geojobs.file.bucket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;

@Slf4j
@Component
@RequiredArgsConstructor
public class BucketObjectCopier {
  private final BucketConf bucketConf;

  public void copy(String sourceKey, String destinationKey) {
    var bucketName = bucketConf.getBucketName();
    bucketConf
        .getS3Client()
        .copyObject(
            CopyObjectRequest.builder()
                .sourceBucket(bucketName)
                .sourceKey(sourceKey)
                .destinationBucket(bucketName)
                .destinationKey(destinationKey)
                .build());
  }

  public void deleteQuietly(String key) {
    try {
      bucketConf
          .getS3Client()
          .deleteObject(
              DeleteObjectRequest.builder().bucket(bucketConf.getBucketName()).key(key).build());
    } catch (RuntimeException e) {
      log.warn("Could not delete bucket object {}", key, e);
    }
  }
}
