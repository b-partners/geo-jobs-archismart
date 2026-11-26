package app.bpartners.geojobs.service;

import static java.lang.System.currentTimeMillis;

import app.bpartners.geojobs.endpoint.rest.model.HealthStatus;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import lombok.SneakyThrows;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public final class ExportExcelWriter {
  private static final String[] HEADERS = {
    "ID", "Adresse ou Nom de zone", "Superficie (m²)", "Date de création", "Statut"
  };

  private ExportExcelWriter() {}

  public record ExportRow(
      String e2Id,
      String zoneName,
      Double areaInSquareMeters,
      Instant creationDatetime,
      HealthStatus status) {}

  @SneakyThrows
  public static File write(List<ExportRow> payloads) {
    File file = Files.createTempFile("detections-" + currentTimeMillis(), ".xlsx").toFile();

    try (Workbook workbook = new XSSFWorkbook();
        FileOutputStream out = new FileOutputStream(file)) {

      Sheet sheet = workbook.createSheet("Detections");
      CreationHelper helper = workbook.getCreationHelper();

      // Styles
      CellStyle headerStyle = workbook.createCellStyle();
      Font headerFont = workbook.createFont();
      headerFont.setBold(true);
      headerStyle.setFont(headerFont);

      CellStyle dateStyle = workbook.createCellStyle();
      dateStyle.setDataFormat(helper.createDataFormat().getFormat("dd-mm-yyyy hh:mm:ss"));

      CellStyle areaStyle = workbook.createCellStyle();
      areaStyle.setDataFormat(helper.createDataFormat().getFormat("#,##0.00"));

      // En-tête
      Row headerRow = sheet.createRow(0);
      for (int i = 0; i < HEADERS.length; i++) {
        Cell cell = headerRow.createCell(i);
        cell.setCellValue(HEADERS[i]);
        cell.setCellStyle(headerStyle);
      }

      // Données
      int rowIndex = 1;
      for (ExportRow payload : payloads) {
        Row row = sheet.createRow(rowIndex++);

        row.createCell(0).setCellValue(payload.e2Id());
        row.createCell(1).setCellValue(payload.zoneName());

        Cell areaCell = row.createCell(2);
        if (payload.areaInSquareMeters() != null) {
          areaCell.setCellValue(payload.areaInSquareMeters());
          areaCell.setCellStyle(areaStyle);
        }

        Cell dateCell = row.createCell(3);
        if (payload.creationDatetime() != null) {
          dateCell.setCellValue(
              LocalDateTime.ofInstant(payload.creationDatetime(), ZoneId.systemDefault()));
          dateCell.setCellStyle(dateStyle);
        }

        row.createCell(4).setCellValue(payload.status().name());
      }

      for (int i = 0; i < HEADERS.length; i++) {
        sheet.autoSizeColumn(i);
      }

      workbook.write(out);
    }

    return file;
  }
}
