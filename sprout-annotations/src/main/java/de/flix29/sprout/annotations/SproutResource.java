package de.flix29.sprout.annotations;

import de.flix29.sprout.annotations.model.Endpoint;

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
     * If {@link SproutResource#tag()}  is not set, this will also be used as the Swagger tag.
     * @return the resource name
     */
    String name() default "";

    /**
     * Optional path (e.g. "/products"). Default is derived as "api/[name]/".
     * @return the resource path
     */
    String path() default "";

    /**
     * If true, only read endpoints are generated for this resource. Note that this overrides the
     * {@link SproutResource#include()} and {@link SproutResource#exclude()} options.<br>
     * e.g., if set to true, no create, update, or delete endpoints will be generated.
     * No matter what is set in include/exclude. You can still exclude read endpoints
     * using {@link SproutResource#exclude()}.
     * @return true if the resource is read-only
     */
    boolean readOnly() default false;

    /**
     * Endpoints to include for this resource. If empty, all endpoints are included by default unless
     * explicitly excluded using {@link SproutResource#exclude()}.
     * @return the endpoints to include
     */
    Endpoint[] include() default {};

    /**
     * Endpoints to exclude for this resource. Bounds stronger than {@link SproutResource#include()} option.
     * @return the endpoints to exclude
     */
    Endpoint[] exclude() default {};

    /**
     * If true, Swagger documentation is generated for this resource.
     * @return true if Swagger docs should be generated
     */
    boolean generateSwaggerDocs() default true;

    /**
     * Optional tag for the resource. This will be used in the Swagger documentation if enabled.
     * @return the resource tag
     */
    String tag() default "";

    /**
     * Optional description for the resource. This will be used in the Swagger documentation if enabled.
     * @return the resource description
     */
    String summary() default "";
}
