package de.flix29.sprout.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to define a Sprout resource.
 * <p>
 * This annotation is used to mark an Entity as a resource in the Sprout
 * framework. These resources are used to generate API endpoints.
 * </p>
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface SproutResource {
    /**
     * Optional name for the resource and generated Classes. Default is the class name.
     */
    String name() default "";

    /**
     * Optional path (e.g. "/products"). Default is derived as "api/[name]/".
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
