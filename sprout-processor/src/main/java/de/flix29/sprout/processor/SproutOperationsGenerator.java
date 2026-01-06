package de.flix29.sprout.processor;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import de.flix29.sprout.annotations.SproutResource;
import de.flix29.sprout.annotations.model.Endpoint;

import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import java.util.Arrays;

class SproutOperationsGenerator {

    private static final ClassName LIST = ClassName.get("java.util", "List");
    private static final ClassName OPTIONAL = ClassName.get("java.util", "Optional");

    private SproutOperationsGenerator() {
        // Utility class
    }

    public static TypeSpec.Builder generateOperations(
            TypeElement type,
            String simpleName,
            SproutResource sproutResource,
            TypeMirror idType
    ) {
        ClassName entityType = ClassName.get(type);
        TypeName idT = TypeName.get(idType);

        TypeSpec.Builder builder = TypeSpec.interfaceBuilder("Sprout" + simpleName + "Operations")
                .addModifiers(Modifier.PUBLIC);

        if (methodGenerationAllowed(Endpoint.GET_ALL, sproutResource)) {
            builder.addMethod(createFindAllMethod(entityType));
        }

        if (methodGenerationAllowed(Endpoint.GET_BY_ID, sproutResource)) {
            builder.addMethod(createFindByIdMethod(entityType, idT));
        }

        if (!sproutResource.readOnly()) {
            if (methodGenerationAllowed(Endpoint.CREATE, sproutResource)) {
                builder.addMethod(createSaveMethod(entityType));
            }
            if (methodGenerationAllowed(Endpoint.UPDATE, sproutResource)) {
                builder.addMethod(createUpdateMethod(entityType, idT));
            }
            if (methodGenerationAllowed(Endpoint.DELETE, sproutResource)) {
                builder.addMethod(createDeleteMethod(idT));
            }
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

    private static boolean methodGenerationAllowed(Endpoint endpoint, SproutResource sproutResource) {
        var excluded = Arrays.asList(sproutResource.exclude());
        if (excluded.contains(endpoint)) {
            return false;
        }
        if (sproutResource.include().length == 0) {
            return true;
        }
        if (sproutResource.readOnly() && (endpoint == Endpoint.GET_ALL || endpoint == Endpoint.GET_BY_ID)) {
            return true;
        }
        return Arrays.asList(sproutResource.include()).contains(endpoint);
    }
}
