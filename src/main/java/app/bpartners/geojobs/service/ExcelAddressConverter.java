package app.bpartners.geojobs.service;

import static app.bpartners.geojobs.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;

import app.bpartners.geojobs.model.exception.ApiException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

@Component
public class ExcelAddressConverter implements Function<File, List<String>> {

  @Override
  public List<String> apply(File file) {
    var addresses = new HashSet<String>();
    try (FileInputStream fis = new FileInputStream(file);
        Workbook workbook = new XSSFWorkbook(fis)) {
      Sheet sheet = workbook.getSheetAt(0);
      for (Row row : sheet) {
        Cell cell = row.getCell(0);
        if (Objects.requireNonNull(cell.getCellType()) == CellType.STRING) {
          addresses.add(cell.getStringCellValue());
        }
      }
      return addresses.stream().toList();
    } catch (IOException e) {
      throw new ApiException(SERVER_EXCEPTION, e);
    }
  }
}
