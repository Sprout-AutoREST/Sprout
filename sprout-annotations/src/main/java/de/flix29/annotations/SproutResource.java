package de.flix29.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface SproutResource {
    /**
     * Optional path (e.g. "products"). Default is derived as "api/[name]/".
     */
    String path() default "";
    /**
     * If true, only read endpoints are generated for this resource.
     */
    boolean readOnly() default false;
    /**
     * Optional description for the resource.
     */
    String summary() default "";
}
