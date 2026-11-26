package app.bpartners.geojobs.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import app.bpartners.geojobs.repository.model.Feature;
import app.bpartners.geojobs.repository.model.cityjson.CityJSONRequest;
import app.bpartners.geojobs.repository.model.cityjson.CityJSONRequestStatus;
import app.bpartners.geojobs.repository.model.detection.FeatureWithDelimitation;
import java.io.FileInputStream;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

class CityJSONRequestConverterTest {
  CityJSONRequestConverter subject = new CityJSONRequestConverter(mock());

  @Test
  void export_rows_with_address_status_and_creation_date() throws Exception {
    var finished =
        CityJSONRequest.builder()
            .id("finished")
            .creationDatetime(Instant.parse("2024-01-10T10:00:00Z"))
            .status(CityJSONRequestStatus.FINISHED)
            .featuresWithDelimitation(
                List.of(
                    new FeatureWithDelimitation(
                        Feature.builder()
                            .properties(new java.util.HashMap<>(Map.of("address", "1 rue A")))
                            .build(),
                        List.of())))
            .build();
    var processing =
        CityJSONRequest.builder()
            .id("processing")
            .status(CityJSONRequestStatus.PROCESSING)
            .externalReference("ref-1")
            .build();

    var file = subject.apply(List.of(finished, processing), List.of());

    try (var workbook = new XSSFWorkbook(new FileInputStream(file))) {
      var sheet = workbook.getSheetAt(0);
      assertEquals("Statut", sheet.getRow(0).getCell(4).getStringCellValue());
      assertEquals("finished", sheet.getRow(1).getCell(0).getStringCellValue());
      assertEquals("1 rue A", sheet.getRow(1).getCell(1).getStringCellValue());
      assertEquals("SUCCEEDED", sheet.getRow(1).getCell(4).getStringCellValue());
      assertEquals("processing", sheet.getRow(2).getCell(0).getStringCellValue());
      assertEquals("ref-1", sheet.getRow(2).getCell(1).getStringCellValue());
      assertEquals("FAILED", sheet.getRow(2).getCell(4).getStringCellValue());
      assertTrue(sheet.getRow(2).getCell(2).getCellType().name().equals("BLANK"));
    }
  }
}
