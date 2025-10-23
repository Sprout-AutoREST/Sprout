package de.flix29.sprout.processor;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;

public class SproutControllerOverrideGenerator {

    private static final ClassName OPTIONAL_CLASS = ClassName.get("java.util", "Optional");
    private static final ClassName RESPONSE_ENTITY_CLASS = ClassName.get("org.springframework.http", "ResponseEntity");
    private static final ClassName LIST_CLASS = ClassName.get("java.util", "List");
    private static final ClassName HTTP_STATUS_CLASS = ClassName.get("org.springframework.http", "HttpStatus");

    private SproutControllerOverrideGenerator() {
        // Utility class
    }

    protected static TypeSpec.Builder generateControllerOverride(
            TypeElement type,
            String simpleName,
            String basePackage,
            boolean readOnly,
            TypeMirror idType
    ) {
        ClassName entityType = ClassName.get(type);
        ClassName serviceType = ClassName.get(basePackage + ".services", "Sprout" + simpleName + "Service");
        TypeName idClassName = TypeName.get(idType);

        TypeSpec.Builder builder = TypeSpec.interfaceBuilder("Sprout" + simpleName + "ControllerOverride")
                .addModifiers(Modifier.PUBLIC);

        addGetAllOverride(builder, entityType, serviceType);
        addGetByIdOverride(builder, entityType, serviceType, idClassName);

        if (!readOnly) {
            addCreateOverride(builder, entityType, serviceType);
            addUpdateOverride(builder, entityType, serviceType, idClassName);
            addDeleteOverride(builder, serviceType, idClassName);
        }

        return builder;
    }

    private static void addGetAllOverride(TypeSpec.Builder builder, ClassName entityType, ClassName serviceType) {
        TypeName listType = ParameterizedTypeName.get(LIST_CLASS, entityType);
        TypeName responseType = ParameterizedTypeName.get(RESPONSE_ENTITY_CLASS, listType);
        TypeName optionalResponse = ParameterizedTypeName.get(OPTIONAL_CLASS, responseType);

        builder.addMethod(MethodSpec.methodBuilder("getAll")
                .addModifiers(Modifier.PUBLIC, Modifier.DEFAULT)
                .addParameter(serviceType, "service")
                .returns(optionalResponse)
                .addStatement("return $T.empty()", OPTIONAL_CLASS)
                .build());

        builder.addMethod(MethodSpec.methodBuilder("defaultGetAll")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addParameter(serviceType, "service")
                .returns(responseType)
                .addStatement("return $T.ok(service.findAll())", RESPONSE_ENTITY_CLASS)
                .build());
    }

    private static void addGetByIdOverride(
            TypeSpec.Builder builder,
            ClassName entityType,
            ClassName serviceType,
            TypeName idType
    ) {
        TypeName responseType = ParameterizedTypeName.get(RESPONSE_ENTITY_CLASS, entityType);
        TypeName optionalResponse = ParameterizedTypeName.get(OPTIONAL_CLASS, responseType);

        builder.addMethod(MethodSpec.methodBuilder("getById")
                .addModifiers(Modifier.PUBLIC, Modifier.DEFAULT)
                .addParameter(idType, "id")
                .addParameter(serviceType, "service")
                .returns(optionalResponse)
                .addStatement("return $T.empty()", OPTIONAL_CLASS)
                .build());

        builder.addMethod(MethodSpec.methodBuilder("defaultGetById")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addParameter(idType, "id")
                .addParameter(serviceType, "service")
                .returns(responseType)
                .addStatement("return service.findById(id)"
                        + "\n        .map($T::ok)"
                        + "\n        .orElse($T.notFound().build())",
                        RESPONSE_ENTITY_CLASS, RESPONSE_ENTITY_CLASS)
                .build());
    }

    private static void addCreateOverride(TypeSpec.Builder builder, ClassName entityType, ClassName serviceType) {
        TypeName responseType = ParameterizedTypeName.get(RESPONSE_ENTITY_CLASS, entityType);
        TypeName optionalResponse = ParameterizedTypeName.get(OPTIONAL_CLASS, responseType);

        builder.addMethod(MethodSpec.methodBuilder("create")
                .addModifiers(Modifier.PUBLIC, Modifier.DEFAULT)
                .addParameter(entityType, "entity")
                .addParameter(serviceType, "service")
                .returns(optionalResponse)
                .addStatement("return $T.empty()", OPTIONAL_CLASS)
                .build());

        builder.addMethod(MethodSpec.methodBuilder("defaultCreate")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addParameter(entityType, "entity")
                .addParameter(serviceType, "service")
                .returns(responseType)
                .addStatement("return $T.status($T.CREATED).body(service.save(entity))",
                        RESPONSE_ENTITY_CLASS, HTTP_STATUS_CLASS)
                .build());
    }

    private static void addUpdateOverride(
            TypeSpec.Builder builder,
            ClassName entityType,
            ClassName serviceType,
            TypeName idType
    ) {
        TypeName responseType = ParameterizedTypeName.get(RESPONSE_ENTITY_CLASS, entityType);
        TypeName optionalResponse = ParameterizedTypeName.get(OPTIONAL_CLASS, responseType);

        builder.addMethod(MethodSpec.methodBuilder("update")
                .addModifiers(Modifier.PUBLIC, Modifier.DEFAULT)
                .addParameter(idType, "id")
                .addParameter(entityType, "entity")
                .addParameter(serviceType, "service")
                .returns(optionalResponse)
                .addStatement("return $T.empty()", OPTIONAL_CLASS)
                .build());

        builder.addMethod(MethodSpec.methodBuilder("defaultUpdate")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addParameter(idType, "id")
                .addParameter(entityType, "entity")
                .addParameter(serviceType, "service")
                .returns(responseType)
                .addStatement("return service.update(id, entity)"
                        + "\n        .map($T::ok)"
                        + "\n        .orElse($T.notFound().build())",
                        RESPONSE_ENTITY_CLASS, RESPONSE_ENTITY_CLASS)
                .build());
    }

    private static void addDeleteOverride(
            TypeSpec.Builder builder,
            ClassName serviceType,
            TypeName idType
    ) {
        TypeName responseType = ParameterizedTypeName.get(RESPONSE_ENTITY_CLASS, TypeName.VOID.box());
        TypeName optionalResponse = ParameterizedTypeName.get(OPTIONAL_CLASS, responseType);

        builder.addMethod(MethodSpec.methodBuilder("deleteById")
                .addModifiers(Modifier.PUBLIC, Modifier.DEFAULT)
                .addParameter(idType, "id")
                .addParameter(serviceType, "service")
                .returns(optionalResponse)
                .addStatement("return $T.empty()", OPTIONAL_CLASS)
                .build());

        builder.addMethod(MethodSpec.methodBuilder("defaultDeleteById")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addParameter(idType, "id")
                .addParameter(serviceType, "service")
                .returns(responseType)
                .addStatement("return service.deleteById(id) ? $T.noContent().build() : $T.notFound().build()",
                        RESPONSE_ENTITY_CLASS, RESPONSE_ENTITY_CLASS)
                .build());
    }
}
