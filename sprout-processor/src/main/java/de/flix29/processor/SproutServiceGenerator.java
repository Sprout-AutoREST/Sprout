package de.flix29.processor;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeSpec;

import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;

public class SproutServiceGenerator {

    protected static TypeSpec.Builder generateService(TypeElement type, String simpleName, TypeMirror idType) {
        return TypeSpec.classBuilder("Sprout" + simpleName + "Service")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(ClassName.get("org.springframework.stereotype", "Service"));
    }
}
