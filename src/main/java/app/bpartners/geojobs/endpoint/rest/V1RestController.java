package app.bpartners.geojobs.endpoint.rest;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Marks a REST controller belonging to API version 1.
 *
 * <p>V1 is currently the default version: its handlers are exposed both at the bare path (e.g.
 * {@code /image}) and under the explicit {@code /v1} prefix (e.g. {@code /v1/image}). The dual
 * exposure comes from the class-level {@link RequestMapping} with paths {@code ""} and {@code
 * "/v1"}, which combine with each method-level mapping.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@RestController
@RequestMapping({"", "/v1"})
public @interface V1RestController {}
