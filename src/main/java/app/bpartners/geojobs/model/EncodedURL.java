package app.bpartners.geojobs.model;

import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public record EncodedURL(String value) {
  public EncodedURL(URL url) {
    this(encode(url.toString()));
  }

  private static String encode(String url) {
    return URLEncoder.encode(url, StandardCharsets.UTF_8);
  }
}
