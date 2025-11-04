package app.bpartners.geojobs.endpoint.rest;

import app.bpartners.geojobs.model.page.PageFromOne;
import org.springframework.core.convert.converter.Converter;

public class PageConverter implements Converter<String, PageFromOne> {
  @Override
  public PageFromOne convert(String source) {
    return new PageFromOne(Integer.parseInt(source));
  }
}
