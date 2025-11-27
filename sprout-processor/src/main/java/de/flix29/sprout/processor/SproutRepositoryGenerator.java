package de.flix29.sprout.processor;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;

public class SproutRepositoryGenerator {

    private static final String SPRING_DATA = "org.springframework.data.";
    private static final String SPRING_DATA_JPA = SPRING_DATA + "jpa.repository";
    private static final ClassName QUERY_CLASS = ClassName.get(SPRING_DATA_JPA, "Query");
    private static final ClassName PARAMETER_CLASS = ClassName.get(SPRING_DATA + "repository.query", "Param");
    private static final ClassName MODIFYING_CLASS = ClassName.get(SPRING_DATA_JPA, "Modifying");
    private static final ClassName TRANSACTIONAL_CLASS =
            ClassName.get("org.springframework.transaction.annotation", "Transactional");

    private SproutRepositoryGenerator() {
        // Utility class
    }

    public static TypeSpec.Builder generateRepository(
            TypeElement type,
            String simpleName,
            String entityName,
            String idName,
            boolean overrideRepository,
            TypeMirror idType
    ) {
        var builder = TypeSpec.interfaceBuilder("Sprout" + simpleName + "Repository")
                .addModifiers(Modifier.PUBLIC)
                .addSuperinterface(ParameterizedTypeName.get(
                        ClassName.get(SPRING_DATA_JPA, "JpaRepository"),
                        ClassName.get(type),
                        TypeName.get(idType)
                ))
                .addMethod(generateDeleteByIdMethod(entityName, idName, TypeName.get(idType)));

        if (overrideRepository) {
            builder.addAnnotation(ClassName.get("org.springframework.data.repository", "NoRepositoryBean"));
        } else {
            builder.addAnnotation(ClassName.get("org.springframework.stereotype", "Repository"));
        }

        return builder;
    }

    private static MethodSpec generateDeleteByIdMethod(String entityName, String idName, TypeName idType) {
        return MethodSpec.methodBuilder("deleteById")
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .addAnnotation(AnnotationSpec.builder(MODIFYING_CLASS)
                        .addMember("clearAutomatically", "$L", true)
                        .addMember("flushAutomatically", "$L", true)
                        .build()
                )
                .addAnnotation(TRANSACTIONAL_CLASS)
                .addAnnotation(AnnotationSpec.builder(QUERY_CLASS)
                        .addMember("value", "$S",
                                "delete from " + entityName + " e where e." + idName + " = :id")
                        .build()
                )
                .addParameter(ParameterSpec.builder(idType, "id")
                        .addAnnotation(AnnotationSpec.builder(PARAMETER_CLASS)
                                .addMember("value", "$S", "id")
                                .build()
                        ).build()
                )
                .returns(TypeName.VOID)
                .build();
    }
}
