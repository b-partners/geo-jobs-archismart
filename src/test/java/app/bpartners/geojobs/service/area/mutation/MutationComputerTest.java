package app.bpartners.geojobs.service.area.mutation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import app.bpartners.geojobs.service.area.mutation.model.AreaPictureHistoryResponse;
import app.bpartners.geojobs.service.area.mutation.model.MutationContext;
import app.bpartners.geojobs.service.area.mutation.model.MutationResponse;
import app.bpartners.geojobs.service.area.mutation.model.MutationResponseStatus;
import app.bpartners.geojobs.service.area.mutation.model.MutationType;
import java.io.File;
import org.junit.jupiter.api.Test;

class MutationComputerTest {
  private static final int MUTATION_MODEL_INPUT_SIZE = 256;

  private final MutationApi apiMock = mock(MutationApi.class);
  private final ImageDownloader imageDownloaderMock = mock(ImageDownloader.class);
  private final ImageResizer imageResizerMock = mock(ImageResizer.class);
  private final MutationComputer subject =
      new MutationComputer(apiMock, imageDownloaderMock, imageResizerMock);

  @Test
  void apply_downloads_resizes_and_delegates_to_the_mutation_api() {
    var older =
        new AreaPictureHistoryResponse.DatedImage(
            2022, new AreaPictureHistoryResponse.PresignedUrl("https://geodata.test/old.jpg"));
    var mostRecent =
        new AreaPictureHistoryResponse.DatedImage(
            2024, new AreaPictureHistoryResponse.PresignedUrl("https://geodata.test/new.jpg"));
    var maskImageFile = new File("mask.png");
    var context = new MutationContext(older, mostRecent, maskImageFile);

    var oldImageFile = new File("old.jpg");
    var recentImageFile = new File("new.jpg");
    when(imageDownloaderMock.download("https://geodata.test/old.jpg")).thenReturn(oldImageFile);
    when(imageDownloaderMock.download("https://geodata.test/new.jpg")).thenReturn(recentImageFile);

    var resizedOld = new File("old-256.png");
    var resizedRecent = new File("new-256.png");
    var resizedMask = new File("mask-256.png");
    when(imageResizerMock.resizePhoto(oldImageFile, MUTATION_MODEL_INPUT_SIZE))
        .thenReturn(resizedOld);
    when(imageResizerMock.resizePhoto(recentImageFile, MUTATION_MODEL_INPUT_SIZE))
        .thenReturn(resizedRecent);
    when(imageResizerMock.resizeMask(maskImageFile, MUTATION_MODEL_INPUT_SIZE))
        .thenReturn(resizedMask);

    var mutationResponse =
        new MutationResponse(
            MutationResponseStatus.SUCCESS, MutationType.DETERIORATION, "some-filename");
    when(apiMock.detectMutation(eq(resizedOld), eq(resizedRecent), eq(resizedMask), any()))
        .thenReturn(mutationResponse);

    var actual = subject.apply(context);

    assertEquals(MutationType.DETERIORATION, actual);
  }
}
