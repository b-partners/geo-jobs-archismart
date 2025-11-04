package app.bpartners.geojobs.model.geometry;

import static java.lang.Integer.parseInt;

import java.util.regex.Pattern;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class TileCoordinatesFromFileName {
  private static final Pattern filename_z_x_y_dot_filetype =
      Pattern.compile("([\\w-]+)_(\\w+)_(\\w+)_(\\w+)\\.(\\w+)");
  private static final Pattern z_x_y_dot_filetype =
      Pattern.compile("(\\w+)_(\\w+)_(\\w+)\\.(\\w+)");

  private final boolean is_z_x_y_dot_filetype;

  public int z(String filename) {
    return extract(filename, is_z_x_y_dot_filetype ? 1 : 2);
  }

  public int x(String filename) {
    return extract(filename, is_z_x_y_dot_filetype ? 2 : 3);
  }

  public int y(String filename) {
    return extract(filename, is_z_x_y_dot_filetype ? 3 : 4);
  }

  private int extract(String filename, int groupPosition) {
    var matcher =
        is_z_x_y_dot_filetype
            ? z_x_y_dot_filetype.matcher(filename)
            : filename_z_x_y_dot_filetype.matcher(filename);
    if (!matcher.matches()) {
      throw new IllegalArgumentException(
          "File name does not follow expected pattern, filename=" + filename);
    }
    return parseInt(matcher.group(groupPosition));
  }
}
