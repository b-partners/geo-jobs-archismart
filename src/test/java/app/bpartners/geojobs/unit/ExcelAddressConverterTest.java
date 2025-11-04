package app.bpartners.geojobs.unit;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.bpartners.geojobs.service.ExcelAddressConverter;
import java.io.File;
import java.util.List;
import lombok.SneakyThrows;
import org.apache.poi.EmptyFileException;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class ExcelAddressConverterTest {
  ExcelAddressConverter subject = new ExcelAddressConverter();

  @SneakyThrows
  @Test
  void convert_addresses() {
    var excelFile = new ClassPathResource("/excel/addresses.xlsx").getFile();
    var excelFileContainingFormula =
        new ClassPathResource("/excel/excel-containing-formula.xlsx").getFile();

    var actual = subject.apply(excelFile);

    assertTrue(subject.apply(excelFileContainingFormula).isEmpty());
    assertTrue(
        actual.containsAll(
            List.of(
                "Adresse",
                "25 avenue Mozart, 75001, Paris, France",
                "1 Rue Benjamin Franklin, 75016 Paris, France")));
  }

  @Test
  void throws_exception_with_bad_excel_file() {
    assertThrows(
        EmptyFileException.class, () -> subject.apply(File.createTempFile("test", ".xlsx")));
  }
}
