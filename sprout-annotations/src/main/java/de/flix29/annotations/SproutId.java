package de.flix29.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to define a Sprout ID field. <br>
 * This annotation is used to mark a field as the ID of an Entity in the Sprout framework.
 * Usually, this is done via the {@link jakarta.persistence.Id} or {@link javax.persistence.Id} annotation.
 * If this annotation is used, the field will be treated as the primary key of the Entity in the generated API. <br>
 * This is only possible in conjunction with the {@link SproutResource} annotation on the Entity class.
 */
@Documented
@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.SOURCE)
public @interface SproutId {

}
