package app.bpartners.geojobs.component;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import app.bpartners.geojobs.file.bucket.BucketConf;
import app.bpartners.geojobs.file.bucket.CustomBucketComponent;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;
import lombok.NonNull;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.transfer.s3.S3TransferManager;
import software.amazon.awssdk.transfer.s3.model.DownloadFileRequest;
import software.amazon.awssdk.transfer.s3.model.FileDownload;

public class CustomBucketComponentTest {
  BucketConf bucketConfMock = mock();
  CustomBucketComponent subject = new CustomBucketComponent(bucketConfMock);

  @Test
  void list_objects_ok() {
    S3Client s3ClientMock = mock();
    S3Object[] s3Objects = mockS3Objects();
    String dummyPath = "dummyPath";
    when(s3ClientMock.listObjects(ListObjectsRequest.builder().bucket(dummyPath).build()))
        .thenReturn(ListObjectsResponse.builder().contents(s3Objects).build());
    when(bucketConfMock.getS3Client()).thenReturn(s3ClientMock);

    var actual = subject.listObjects(dummyPath);

    assertEquals(Arrays.stream(s3Objects).toList(), actual);
  }

  @Test
  void list_objects_prefix_ok() {
    S3Client s3ClientMock = mock();
    S3Object[] s3Objects = mockS3Objects();
    String dummyPath = "dummyPath";
    String dummyPrefix = "dummyPrefix";
    String dummyContinuationToken = "dummyContinuationToken";
    ListObjectsV2Request listObjectsV2Request1 = mockV2Request(dummyPath, dummyPrefix, null);
    ListObjectsV2Request listObjectsV2Request2 =
        mockV2Request(dummyPath, dummyPrefix, dummyContinuationToken);
    when(s3ClientMock.listObjectsV2(listObjectsV2Request1))
        .thenReturn(mockV2Response(s3Objects, dummyContinuationToken));
    when(s3ClientMock.listObjectsV2(listObjectsV2Request2))
        .thenReturn(mockV2Response(s3Objects, null));
    when(bucketConfMock.getS3Client()).thenReturn(s3ClientMock);

    var actual = subject.listObjects(dummyPath, dummyPrefix);

    assertEquals(
        Stream.concat(Arrays.stream(s3Objects), Arrays.stream(s3Objects)).toList(), actual);
  }

  @Test
  void download_file_ok() {
    String bucketName = "bucketName";
    String bucketKey = "bucketKey";
    S3TransferManager s3TransferManagerMock = mock();
    FileDownload mockFileDownload = mock();
    when(mockFileDownload.completionFuture()).thenReturn(CompletableFuture.completedFuture(null));
    when(s3TransferManagerMock.downloadFile(any(DownloadFileRequest.class)))
        .thenReturn(mockFileDownload);

    when(bucketConfMock.getS3TransferManager()).thenReturn(s3TransferManagerMock);

    var actual = subject.download(bucketName, bucketKey);

    assertNotNull(actual);
  }

  @NonNull
  private static S3Object[] mockS3Objects() {
    return new S3Object[] {
      S3Object.builder().key("mockedObject1").build(),
      S3Object.builder().key("mockedObject2").build()
    };
  }

  private static ListObjectsV2Response mockV2Response(S3Object[] s3Objects, String nexToken) {
    return ListObjectsV2Response.builder()
        .contents(s3Objects)
        .nextContinuationToken(nexToken)
        .build();
  }

  private static ListObjectsV2Request mockV2Request(
      String dummyPath, String dummyPrefix, String dummyToken) {
    return ListObjectsV2Request.builder()
        .bucket(dummyPath)
        .prefix(dummyPrefix)
        .continuationToken(dummyToken)
        .build();
  }
}
