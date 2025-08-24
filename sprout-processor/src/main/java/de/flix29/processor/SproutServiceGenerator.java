package de.flix29.processor;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;

import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;

public class SproutServiceGenerator {

    private SproutServiceGenerator() {
        // Utility class
    }

    protected static TypeSpec.Builder generateService(
            TypeElement type, String simpleName, String basePath, TypeMirror idType
    ) {
        final ClassName repository = ClassName.get(basePath + ".repositories", "Sprout" + simpleName + "Repository");

        return TypeSpec.classBuilder("Sprout" + simpleName + "Service")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(ClassName.get("org.springframework.stereotype", "Service"))
                .addField(FieldSpec.builder(
                        repository,
                        "repository", Modifier.PRIVATE, Modifier.FINAL
                ).build())
                .addMethod(MethodSpec.constructorBuilder()
                        .addParameter(repository, "repository")
                        .addStatement("this.repository = repository")
                        .build()
                );
    }
}
