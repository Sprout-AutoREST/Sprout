package de.flix29.sprout.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to specify access control policies for CRUD operations on a resource.
 * Each operation (read, create, update, delete) can have its own policy string.
 * If no policy is needed for an operation, the corresponding string can be left empty.
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface SproutPolicy {
    /**
     * The policy string for the read operation. Can be empty if no policy is needed.
     */
    String read() default "";

    /**
     * The policy string for the create operation. Can be empty if no policy is needed.
     */
    String create() default "";

    /**
     * The policy string for the update operation. Can be empty if no policy is needed.
     */
    String update() default "";

    /**
     * The policy string for the delete operation. Can be empty if no policy is needed.
     */
    String delete() default "";
}
