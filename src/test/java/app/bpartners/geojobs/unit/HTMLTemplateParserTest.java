package app.bpartners.geojobs.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;

import app.bpartners.geojobs.template.HTMLTemplateParser;
import java.lang.reflect.Field;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

class HTMLTemplateParserTest {

  @SneakyThrows
  @Test
  void apply_ok() {
    var expected =
        """
        <html lang="fr">
        <head>
            <title>Template</title>
        </head>
        <body>
        </body>
        </html>""";
    HTMLTemplateParser subject = new HTMLTemplateParser();

    var actual = subject.apply("template", new Context());

    assertEquals(expected, actual);
    Field templateEngineField = subject.getClass().getDeclaredField("templateEngine");
    templateEngineField.setAccessible(true);
    TemplateEngine templateEngine = (TemplateEngine) templateEngineField.get(subject);
    ClassLoaderTemplateResolver templateResolverCaptured =
        (ClassLoaderTemplateResolver)
            templateEngine.getTemplateResolvers().stream().toList().getFirst();
    assertEquals("/templates/", templateResolverCaptured.getPrefix());
    assertEquals(".html", templateResolverCaptured.getSuffix());
    assertEquals("UTF-8", templateResolverCaptured.getCharacterEncoding());
  }
}
