package de.flix29.processor;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;

public class SproutRepositoryGenerator {

    private SproutRepositoryGenerator() {
        // Utility class
    }

    public static TypeSpec.Builder generateRepository(TypeElement type, String simpleName, TypeMirror idType) {
        return TypeSpec.interfaceBuilder("Sprout" + simpleName + "Repository")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(ClassName.get("org.springframework.stereotype", "Repository"))
                .addSuperinterface(ParameterizedTypeName.get(
                        ClassName.get("org.springframework.data.jpa.repository", "JpaRepository"),
                        ClassName.get(type),
                        TypeName.get(idType)
                ));
    }
}
