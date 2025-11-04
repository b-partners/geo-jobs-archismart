package app.bpartners.geojobs.template;

import static org.thymeleaf.templatemode.TemplateMode.HTML;

import java.util.function.BiFunction;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

@Component
public class HTMLTemplateParser implements BiFunction<String, Context, String> {
  private final TemplateEngine templateEngine;

  public HTMLTemplateParser() {
    templateEngine = configureEngine();
  }

  private TemplateEngine configureEngine() {
    var templateResolver = new ClassLoaderTemplateResolver();
    templateResolver.setPrefix("/templates/");
    templateResolver.setSuffix(".html");
    templateResolver.setCharacterEncoding("UTF-8");
    templateResolver.setTemplateMode(HTML);

    TemplateEngine templateEngine = new TemplateEngine();
    templateEngine.setTemplateResolver(templateResolver);
    return templateEngine;
  }

  @Override
  public String apply(String template, Context context) {
    return templateEngine.process(template, context);
  }
}
