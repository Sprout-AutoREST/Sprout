package de.flix29.sprout.processor;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;

public class SproutServiceGenerator {

    private static final ClassName LIST = ClassName.get("java.util", "List");
    private static final ClassName OPTIONAL = ClassName.get("java.util", "Optional");
    private static final ClassName BEAN_UTILS = ClassName.get("org.springframework.beans", "BeanUtils");
    private static final ClassName SERVICE = ClassName.get("org.springframework.stereotype", "Service");
    private static final ClassName TRANSACTIONAL =
            ClassName.get("org.springframework.transaction.annotation", "Transactional");

    private SproutServiceGenerator() {
        // Utility class
    }

    protected static TypeSpec.Builder generateService(
            TypeElement type, String simpleName, String basePath, boolean readOnly, TypeMirror idType
    ) {
        final String componentName = "Sprout" + simpleName;
        final ClassName repository = ClassName.get(basePath + ".repositories", componentName + "Repository");
        final ClassName entityType = ClassName.get(type);
        final TypeName idT = TypeName.get(idType);

        var builder = TypeSpec.classBuilder(componentName + "Service")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(SERVICE)
                .addField(FieldSpec.builder(
                        repository, "repository", Modifier.PRIVATE, Modifier.FINAL
                ).build())
                .addMethod(MethodSpec.constructorBuilder()
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(repository, "repository")
                        .addStatement("this.repository = repository")
                        .build()
                )
                .addMethod(generateFindAllMethod(entityType))
                .addMethod(generateFindByIdMethod(entityType, idT));

        if (!readOnly) {
            builder
                    .addMethod(generateSaveMethod(entityType))
                    .addMethod(generateUpdateMethod(entityType, idT))
                    .addMethod(generateDeleteMethod(idT));
        }
        if (readOnly) {
            builder.addAnnotation(AnnotationSpec.builder(TRANSACTIONAL)
                    .addMember("readOnly", "$L", true)
                    .build()
            );
        }
        return builder;
    }

    private static MethodSpec generateFindAllMethod(ClassName entityType) {
        return MethodSpec.methodBuilder("findAll")
                .addModifiers(Modifier.PUBLIC)
                .returns(ParameterizedTypeName.get(LIST, entityType))
                .addStatement("return repository.findAll()")
                .build();
    }

    private static MethodSpec generateFindByIdMethod(ClassName entityType, TypeName idT) {
        return MethodSpec.methodBuilder("findById")
                .addModifiers(Modifier.PUBLIC)
                .addParameter(idT, "id")
                .returns(ParameterizedTypeName.get(OPTIONAL, entityType))
                .addStatement("return repository.findById(id)")
                .build();
    }

    private static MethodSpec generateSaveMethod(ClassName entityType) {
        return MethodSpec.methodBuilder("save")
                .addAnnotation(TRANSACTIONAL)
                .addModifiers(Modifier.PUBLIC)
                .addParameter(entityType, "entity")
                .returns(entityType)
                .addStatement("return repository.save(entity)")
                .build();
    }

    private static MethodSpec generateUpdateMethod(ClassName entityType, TypeName idT) {
        return MethodSpec.methodBuilder("update")
                .addAnnotation(TRANSACTIONAL)
                .addModifiers(Modifier.PUBLIC)
                .addParameter(idT, "id")
                .addParameter(entityType, "entity")
                .returns(ParameterizedTypeName.get(OPTIONAL, entityType))
                .addStatement("return repository.findById(id).map(existing -> { " +
                        "$T.copyProperties(entity, existing, $S); " +
                        "return repository.save(existing); " +
                        "})", BEAN_UTILS, "id")
                .build();
    }

    private static MethodSpec generateDeleteMethod(TypeName idT) {
        return MethodSpec.methodBuilder("deleteById")
                .addAnnotation(TRANSACTIONAL)
                .addModifiers(Modifier.PUBLIC)
                .addParameter(idT, "id")
                .returns(TypeName.BOOLEAN)
                .beginControlFlow("if (!repository.existsById(id))")
                .addStatement("return false")
                .endControlFlow()
                .addStatement("repository.deleteById(id)")
                .addStatement("return true")
                .build();
    }
}
