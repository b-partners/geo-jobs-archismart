package app.bpartners.geojobs.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.bpartners.geojobs.service.ExcelAddressConverter;
import java.io.File;
import java.io.FileOutputStream;
import java.util.List;
import lombok.SneakyThrows;
import org.apache.poi.EmptyFileException;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class ExcelAddressConverterTest {
  ExcelAddressConverter subject = new ExcelAddressConverter();

  @SneakyThrows
  private static File multiSheetExcelFile() {
    var file = File.createTempFile("multi-sheet-addresses-", ".xlsx");
    try (var workbook = new XSSFWorkbook();
        var outputStream = new FileOutputStream(file)) {
      for (int sheetNumber = 1; sheetNumber <= 3; sheetNumber++) {
        var sheet = workbook.createSheet("sheet-" + sheetNumber);
        sheet.createRow(0).createCell(0).setCellValue("address of sheet " + sheetNumber);
      }
      workbook.write(outputStream);
    }
    return file;
  }

  @SneakyThrows
  @Test
  void convert_addresses() {
    var excelFile = new ClassPathResource("/excel/addresses.xlsx").getFile();
    var excelFileContainingFormula =
        new ClassPathResource("/excel/excel-containing-formula.xlsx").getFile();

    var actual = subject.apply(excelFile, 1);

    assertTrue(subject.apply(excelFileContainingFormula, 1).isEmpty());
    assertTrue(
        actual.containsAll(
            List.of(
                "Adresse",
                "25 avenue Mozart, 75001, Paris, France",
                "1 Rue Benjamin Franklin, 75016 Paris, France")));
  }

  @Test
  void convert_addresses_of_null_sheet_index_reads_first_sheet() {
    var actual = subject.apply(multiSheetExcelFile(), null);

    assertEquals(List.of("address of sheet 1"), actual);
  }

  @Test
  void convert_addresses_of_first_sheet_index_reads_first_sheet() {
    var actual = subject.apply(multiSheetExcelFile(), 1);

    assertEquals(List.of("address of sheet 1"), actual);
  }

  @Test
  void convert_addresses_of_given_sheet_index_reads_matching_sheet() {
    var excelFile = multiSheetExcelFile();

    assertEquals(List.of("address of sheet 2"), subject.apply(excelFile, 2));
    assertEquals(List.of("address of sheet 3"), subject.apply(excelFile, 3));
  }

  @Test
  void convert_addresses_of_out_of_bound_sheet_index_ko() {
    var excelFile = multiSheetExcelFile();

    assertThrows(IllegalArgumentException.class, () -> subject.apply(excelFile, 4));
    assertThrows(IllegalArgumentException.class, () -> subject.apply(excelFile, 0));
  }

  @Test
  void throws_exception_with_bad_excel_file() {
    assertThrows(
        EmptyFileException.class, () -> subject.apply(File.createTempFile("test", ".xlsx"), 1));
  }
}
