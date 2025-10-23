package de.flix29.sprout.processor;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;

final class SproutOperationsGenerator {

    private static final ClassName LIST = ClassName.get("java.util", "List");
    private static final ClassName OPTIONAL = ClassName.get("java.util", "Optional");

    private SproutOperationsGenerator() {
        // Utility class
    }

    protected static TypeSpec.Builder generateOperations(
            TypeElement type,
            String simpleName,
            boolean readOnly,
            TypeMirror idType
    ) {
        ClassName entityType = ClassName.get(type);
        TypeName idT = TypeName.get(idType);

        TypeSpec.Builder builder = TypeSpec.interfaceBuilder("Sprout" + simpleName + "Operations")
                .addModifiers(Modifier.PUBLIC);

        builder
                .addMethod(createFindAllMethod(entityType))
                .addMethod(createFindByIdMethod(entityType, idT));

        if (!readOnly) {
            builder
                    .addMethod(createSaveMethod(entityType))
                    .addMethod(createUpdateMethod(entityType, idT))
                    .addMethod(createDeleteMethod(idT));
        }

        return builder;
    }

    private static MethodSpec createFindAllMethod(ClassName entityType) {
        return MethodSpec.methodBuilder("findAll")
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .returns(ParameterizedTypeName.get(LIST, entityType))
                .build();
    }

    private static MethodSpec createFindByIdMethod(ClassName entityType, TypeName idT) {
        return MethodSpec.methodBuilder("findById")
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .addParameter(idT, "id")
                .returns(ParameterizedTypeName.get(OPTIONAL, entityType))
                .build();
    }

    private static MethodSpec createSaveMethod(ClassName entityType) {
        return MethodSpec.methodBuilder("save")
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .addParameter(entityType, "entity")
                .returns(entityType)
                .build();
    }

    private static MethodSpec createUpdateMethod(ClassName entityType, TypeName idT) {
        return MethodSpec.methodBuilder("update")
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .addParameter(idT, "id")
                .addParameter(entityType, "entity")
                .returns(ParameterizedTypeName.get(OPTIONAL, entityType))
                .build();
    }

    private static MethodSpec createDeleteMethod(TypeName idT) {
        return MethodSpec.methodBuilder("deleteById")
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .addParameter(idT, "id")
                .returns(TypeName.BOOLEAN)
                .build();
    }
}
