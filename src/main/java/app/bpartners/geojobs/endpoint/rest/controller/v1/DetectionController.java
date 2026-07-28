package app.bpartners.geojobs.endpoint.rest.controller.v1;

import static app.bpartners.geojobs.file.ExtensionGuesser.OFFICE_OPEN_XML_FILE_MEDIA_TYPE;

import app.bpartners.geojobs.endpoint.rest.V1RestController;
import app.bpartners.geojobs.endpoint.rest.controller.mapper.CreateDetectionMapper;
import app.bpartners.geojobs.endpoint.rest.controller.mapper.DetectionFileObjectMapper;
import app.bpartners.geojobs.endpoint.rest.controller.v1.mapper.DetectionRestMapper;
import app.bpartners.geojobs.endpoint.rest.controller.v1.mapper.DetectionSurfaceUnitMapper;
import app.bpartners.geojobs.endpoint.rest.model.*;
import app.bpartners.geojobs.endpoint.rest.security.AuthProvider;
import app.bpartners.geojobs.endpoint.rest.security.authorizer.DetectionAuthorizer;
import app.bpartners.geojobs.endpoint.rest.validator.CreateDetectionExportRequestValidator;
import app.bpartners.geojobs.file.FileWriter;
import app.bpartners.geojobs.file.MediaTypeGuesser;
import app.bpartners.geojobs.model.exception.BadRequestException;
import app.bpartners.geojobs.model.exception.NotImplementedException;
import app.bpartners.geojobs.model.page.BoundedPageSize;
import app.bpartners.geojobs.model.page.PageFromOne;
import app.bpartners.geojobs.repository.CommunityAuthorizationRepository;
import app.bpartners.geojobs.repository.model.community.CommunityAuthorization;
import app.bpartners.geojobs.service.CommunityUsedSurfaceService;
import app.bpartners.geojobs.service.DetectionExportService;
import app.bpartners.geojobs.service.DetectionService;
import app.bpartners.geojobs.validator.ConfigureAddressValidator;
import app.bpartners.geojobs.validator.CreateDetectionValidator;
import app.bpartners.geojobs.validator.GetUsageValidator;
import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@V1RestController
@RequiredArgsConstructor
public class DetectionController {
  private final DetectionExportService detectionExportService;
  private final CreateDetectionExportRequestValidator createDetectionExportRequestValidator;
  private final CommunityUsedSurfaceService communityUsedSurfaceService;
  private final GetUsageValidator getUsageValidator;
  private final CommunityAuthorizationRepository communityAuthRepository;
  private final AuthProvider authProvider;
  private final DetectionSurfaceUnitMapper unitMapper;
  private final DetectionAuthorizer detectionAuthorizer;
  private final FileWriter fileWriter;
  private final MediaTypeGuesser mediaTypeGuesser;
  private final ConfigureAddressValidator configureAddressValidator;
  private final CreateDetectionValidator createDetectionValidator;
  private final DetectionService detectionService;
  private final DetectionFileObjectMapper detectionFileObjectMapper;
  private final CreateDetectionMapper createDetectionMapper;
  private final DetectionRestMapper detectionRestMapper;

  @PostMapping("/detections/{id}/vgg")
  public Detection computeDetectionVgg(@PathVariable(name = "id") String detectionIdentifier) {
    return detectionRestMapper.toRest(detectionService.producesVggComputing(detectionIdentifier));
  }

  @PutMapping("/communities/{id}/detectionsExport/{exportId}")
  public DetectionExportRequest exportDetections(
      @PathVariable(name = "id") String communityIdentifier,
      @PathVariable(name = "exportId") String exportId,
      @RequestBody CreateDetectionExportRequest createDetectionExportRequest) {
    createDetectionExportRequestValidator.accept(createDetectionExportRequest);

    var presignUrl =
        detectionExportService.requestDetectionExport(
            communityIdentifier,
            exportId,
            createDetectionExportRequest.getFrom(),
            createDetectionExportRequest.getTo(),
            createDetectionExportRequest.getAdditionalExportedAttributes());

    return new DetectionExportRequest()
        .id(exportId)
        .from(createDetectionExportRequest.getFrom())
        .to(createDetectionExportRequest.getTo())
        .additionalExportedAttributes(
            createDetectionExportRequest.getAdditionalExportedAttributes())
        .fileUrl(presignUrl);
  }

  @PostMapping("/communities/{communityId}/detections/{id}/fileResult")
  public Detection configureDetectionFileResult(
      @PathVariable(name = "communityId") String communityOwnerId,
      @PathVariable(name = "id") String detectionId,
      @RequestPart(value = "file") MultipartFile file,
      @RequestParam(value = "extensionType", defaultValue = "zip") String extensionType)
      throws IOException {
    return detectionRestMapper.toRest(
        detectionService.configureFileResult(communityOwnerId, detectionId, file, extensionType));
  }

  @PutMapping("/communities/{communityId}/detections/{id}/step")
  public Detection updateCommunityDetectionStep(
      @PathVariable(name = "communityId") String communityOwnerId,
      @PathVariable(name = "id") String detectionId,
      @RequestBody DetectionStep step) {
    return detectionRestMapper.toRest(
        detectionService.updateDetectionStep(detectionId, communityOwnerId, step));
  }

  @GetMapping("/detections/{id}/fileObjects")
  public List<DetectionFileObject> getDetectionFileObjects(
      @PathVariable(name = "id") String detectionId) {
    return detectionService.getDetectionFileObjects(detectionId).stream()
        .map(detectionFileObjectMapper::toRest)
        .toList();
  }

  @PostMapping("/detections/{id}")
  public Detection processDetection(
      @PathVariable(name = "id") String detectionId,
      @RequestBody CreateDetectionDebugMode createDetectionDebugMode) {
    var createDetection = createDetectionMapper.fromDebugMode(createDetectionDebugMode);
    createDetectionValidator.accept(createDetection);
    detectionAuthorizer.accept(detectionId, createDetection, authProvider.getPrincipal());
    var communityAuthorization =
        communityAuthRepository.findByApiKey(authProvider.getPrincipal().getPassword());
    var communityOwnerId = communityAuthorization.map(CommunityAuthorization::getId).orElse(null);
    return detectionRestMapper.toRest(
        detectionService.processDetection(
            detectionId,
            createDetection,
            communityOwnerId,
            createDetectionDebugMode.getDebugMode()));
  }

  @PostMapping("/detections/{id}/geojson")
  public Detection finalizeShapeConfig(
      @PathVariable(name = "id") String detectionId, @RequestBody byte[] featuresFromShape) {
    File featuresFile = fileWriter.apply(featuresFromShape, null);
    return detectionRestMapper.toRest(
        detectionService.finalizeGeoJsonConfig(detectionId, featuresFile));
  }

  @PostMapping("/detections/{id}/shape")
  public Detection configureDetectionShapeFile(
      @PathVariable(name = "id") String detectionId, @RequestBody byte[] shapeFileAsBytes) {
    File shapeFile = fileWriter.apply(shapeFileAsBytes, null);
    return detectionRestMapper.toRest(detectionService.configureShapeFile(detectionId, shapeFile));
  }

  @PostMapping("/detections/{id}/image")
  public Detection configureRooferDetectionImageFile(
      @PathVariable(name = "id") String detectionId, @RequestBody byte[] imageAsByte) {
    throw new NotImplementedException("POST /detections/{id}/image not supported anymore.");
  }

  @PostMapping("/detections/{id}/pdf")
  public Detection uploadRooferDetectionPdfResult(
      @PathVariable(name = "id") String detectionId, @RequestBody byte[] pdfAsByte) {
    File imageFile = fileWriter.apply(pdfAsByte, null);
    return detectionRestMapper.toRest(detectionService.uploadPdfFile(detectionId, imageFile));
  }

  @PostMapping("/detections/{id}/excel")
  public Detection configureDetectionExcelFile(
      @PathVariable(name = "id") String detectionId, @RequestBody byte[] excelFileAsBytes) {
    var excelFile = fileWriter.apply(excelFileAsBytes, null);
    var guessedMediaType = mediaTypeGuesser.apply(excelFileAsBytes);
    if (!OFFICE_OPEN_XML_FILE_MEDIA_TYPE.equals(guessedMediaType)) {
      throw new BadRequestException(
          "Only open office file (docx, xlsx, xls) media type is accepted but provided was : "
              + guessedMediaType);
    }
    return detectionRestMapper.toRest(detectionService.configureExcelFile(detectionId, excelFile));
  }

  @PutMapping("/detections/{id}/step")
  public Detection updateDetectionStep(
      @PathVariable(name = "id") String detectionId, @RequestBody DetectionStep step) {
    return detectionRestMapper.toRest(
        detectionService.updateDetectionStep(detectionId, null, step));
  }

  @PostMapping("/detections/{id}/addresses")
  public Detection configureDetectionAddresses(
      @PathVariable(name = "id") String detectionId, @RequestBody List<Address> addresses) {
    configureAddressValidator.accept(addresses);
    return detectionRestMapper.toRest(
        detectionService.configureDetectionAddresses(
            detectionId, addresses.stream().map(Address::getAddress).toList()));
  }

  @PostMapping("/detections/{id}/roofer")
  public Detection processRooferDetection(
      @PathVariable(name = "id") String detectionId, @RequestBody CreateDetection createDetection) {
    throw new NotImplementedException("POST /detections/{id}/roofer not supported anymore.");
  }

  @PostMapping("/detections/{id}/sync")
  public Detection processDetectionSynchronously(
      @PathVariable(name = "id") String detectionId,
      @RequestBody CreateDetectionDebugMode createDetectionDebugMode) {
    var createDetection = createDetectionMapper.fromDebugMode(createDetectionDebugMode);
    createDetectionValidator.accept(createDetection);
    detectionAuthorizer.accept(detectionId, createDetection, authProvider.getPrincipal());
    var communityAuthorization =
        communityAuthRepository.findByApiKey(authProvider.getPrincipal().getPassword());
    var communityOwnerId = communityAuthorization.map(CommunityAuthorization::getId).orElse(null);
    return detectionRestMapper.toRest(
        detectionService.processDetectionSynchronously(
            detectionId,
            createDetection,
            communityOwnerId,
            createDetectionDebugMode.getDebugMode()));
  }

  @PutMapping("/detections/{id}/roofs/properties")
  public Detection computeDetectionRoofsProperties(
      @PathVariable(name = "id") String detectionE2Id) {
    return detectionRestMapper.toRest(detectionService.computeRoofsProperties(detectionE2Id));
  }

  @PostMapping("/detections/{id}/roofDelimiter")
  public Detection configureDetectionRoofDelimiter(
      @PathVariable(name = "id") String detectionId, @RequestBody RoofDelimiter roofDelimiter) {
    throw new NotImplementedException("POST /detections/{id}/roofDelimiter not supported anymore.");
  }

  @PostMapping("/detections/{id}/roofer/email")
  public Detection sendMailAboutProspect(
      @PathVariable(name = "id") String detectionId, @RequestBody Prospect prospect) {
    detectionAuthorizer.accept(detectionId, authProvider.getPrincipal());
    return detectionRestMapper.toRest(
        detectionService.sendMailAboutProspect(detectionId, prospect));
  }

  @GetMapping("/detections/{id}")
  public Detection getProcessedDetection(@PathVariable(name = "id") String detectionId) {
    detectionAuthorizer.accept(detectionId, authProvider.getPrincipal());
    return detectionRestMapper.toRest(detectionService.getProcessedDetection(detectionId));
  }

  @GetMapping("/usage")
  public DetectionUsage getDetectionUsage(
      @RequestParam(name = "surfaceUnit", required = false) DetectionSurfaceUnit surfaceUnit) {
    getUsageValidator.accept(authProvider.getPrincipal());
    return communityUsedSurfaceService.getUsage(
        authProvider.getPrincipal(), unitMapper.toDomain(surfaceUnit));
  }

  @GetMapping("/detections")
  public List<Detection> getDetections(
      @RequestParam(name = "page", defaultValue = "1", required = false) PageFromOne page,
      @RequestParam(name = "pageSize", defaultValue = "10", required = false)
          BoundedPageSize pageSize,
      @RequestParam(name = "from", required = false, defaultValue = "") Instant from,
      @RequestParam(name = "to", required = false) Instant to,
      @RequestParam(name = "zoneName", required = false) String zoneName) {
    var communityAuthorization =
        communityAuthRepository.findByApiKey(authProvider.getPrincipal().getPassword());
    var communityOwnerId = communityAuthorization.map(CommunityAuthorization::getId);
    Optional<String> optionalZoneName = zoneName == null ? Optional.empty() : Optional.of(zoneName);
    return detectionRestMapper.toRest(
        detectionService.getDetectionsByCriteria(
            communityOwnerId, page, pageSize, from, to, optionalZoneName));
  }
}
