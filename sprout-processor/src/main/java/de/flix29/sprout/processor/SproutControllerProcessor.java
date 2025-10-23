package de.flix29.sprout.processor;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import de.flix29.sprout.annotations.SproutPolicy;

import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;

public class SproutControllerProcessor {

    private static final String SPRING_WEB_ANNOTATION_PACKAGE = "org.springframework.web.bind.annotation";
    private static final ClassName PATH_VARIABLE_CLASS = ClassName.get(SPRING_WEB_ANNOTATION_PACKAGE, "PathVariable");
    private static final ClassName RESPONSE_ENTITY_CLASS = ClassName.get("org.springframework.http", "ResponseEntity");
    private static final ClassName LIST_CLASS = ClassName.get("java.util", "List");
    private static final ClassName OPTIONAL_CLASS = ClassName.get("java.util", "Optional");
    private static final ClassName OBJECT_PROVIDER_CLASS =
            ClassName.get("org.springframework.beans.factory", "ObjectProvider");
    private static final String APPLICATION_JSON = "application/json";
    private static final String ID = "/{id}";
    private static final String VALUE = "value";

    private SproutControllerProcessor() {
        // Utility class
    }

    protected static TypeSpec.Builder generateController(
            TypeElement type,
            String simpleName,
            String basePackage,
            boolean readOnly,
            SproutPolicy policy,
            String apiPath,
            TypeMirror idType
    ) {
        final String componentName = "Sprout" + simpleName;
        ClassName service = ClassName.get(basePackage + ".services", componentName + "Service");
        ClassName overrideType = ClassName.get(basePackage + ".controllers", componentName + "ControllerOverride");
        TypeName overridesList = ParameterizedTypeName.get(LIST_CLASS, overrideType);
        var typeSpec = TypeSpec.classBuilder(componentName + "Controller")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(ClassName.get(SPRING_WEB_ANNOTATION_PACKAGE, "RestController"))
                .addAnnotation(AnnotationSpec
                        .builder(ClassName.get(SPRING_WEB_ANNOTATION_PACKAGE, "RequestMapping"))
                        .addMember("path", "$S", apiPath)
                        .addMember("produces", "$S", APPLICATION_JSON)
                        .build()
                )
                .addField(FieldSpec.builder(
                                service, "service",
                                Modifier.PRIVATE, Modifier.FINAL
                        ).build()
                )
                .addField(FieldSpec.builder(
                        overridesList, "overrides",
                        Modifier.PRIVATE, Modifier.FINAL
                ).build())
                .addMethod(MethodSpec.constructorBuilder()
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(service, "service")
                        .addParameter(ParameterizedTypeName.get(OBJECT_PROVIDER_CLASS, overrideType),
                                "overridesProvider")
                        .addStatement("this.service = service")
                        .addStatement("this.overrides = overridesProvider.orderedStream().toList()")
                        .build()
                )
                .addMethod(generateGetAllMethod(
                        type,
                        simpleName,
                        policy == null ? null : policy.read(),
                        overrideType
                ))
                .addMethod(generateGetByIdMethod(
                        type,
                        simpleName,
                        idType,
                        policy == null ? null : policy.read(),
                        overrideType
                ));

        if (!readOnly) {
            typeSpec
                    .addMethod(generatePostMethod(
                            type,
                            simpleName,
                            policy == null ? null : policy.create(),
                            overrideType
                    ))
                    .addMethod(generatePutMethod(
                            type,
                            simpleName,
                            idType,
                            policy == null ? null : policy.update(),
                            overrideType
                    ))
                    .addMethod(generateDeleteMethod(
                            simpleName,
                            idType,
                            policy == null ? null : policy.delete(),
                            overrideType
                    ));
        }

        return typeSpec;
    }

    private static MethodSpec generateGetAllMethod(
            TypeElement type,
            String simpleName,
            String policy,
            ClassName overrideType
    ) {
        var methodSpec = MethodSpec.methodBuilder("getAll")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(AnnotationSpec
                        .builder(ClassName.get(SPRING_WEB_ANNOTATION_PACKAGE, "GetMapping"))
                        .build()
                )
                .returns(ParameterizedTypeName.get(
                        RESPONSE_ENTITY_CLASS,
                        ParameterizedTypeName.get(
                                LIST_CLASS,
                                ClassName.get(type)
                        )
                ))
                .addJavadoc("Returns all $L items.\n", simpleName)
                .beginControlFlow("for (var override : overrides)")
                .addStatement("$T response = override.getAll(service)",
                        ParameterizedTypeName.get(
                                OPTIONAL_CLASS,
                                ParameterizedTypeName.get(
                                        RESPONSE_ENTITY_CLASS,
                                        ParameterizedTypeName.get(LIST_CLASS, ClassName.get(type))
                                )
                        )
                )
                .beginControlFlow("if (response.isPresent())")
                .addStatement("return response.get()")
                .endControlFlow()
                .endControlFlow()
                .addStatement("return $T.defaultGetAll(service)", overrideType);

        if (policy != null && !policy.isBlank()) {
            methodSpec.addAnnotation(generatePreAuthorizeAnnotation(policy));
        }

        return methodSpec.build();
    }

    private static MethodSpec generateGetByIdMethod(
            TypeElement type,
            String simpleName,
            TypeMirror idType,
            String policy,
            ClassName overrideType
    ) {
        var methodSpec = MethodSpec.methodBuilder("getById")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(AnnotationSpec
                        .builder(ClassName.get(SPRING_WEB_ANNOTATION_PACKAGE, "GetMapping"))
                        .addMember("path", "$S", ID)
                        .build()
                )
                .addParameter(ParameterSpec.builder(TypeName.get(idType), "id")
                        .addAnnotation(PATH_VARIABLE_CLASS)
                        .build()
                )
                .returns(ParameterizedTypeName.get(
                        RESPONSE_ENTITY_CLASS,
                        ClassName.get(type)
                ))
                .addJavadoc("Returns a single $L item by its ID.\n", simpleName)
                .beginControlFlow("for (var override : overrides)")
                .addStatement("$T response = override.getById(id, service)",
                        ParameterizedTypeName.get(
                                OPTIONAL_CLASS,
                                ParameterizedTypeName.get(
                                        RESPONSE_ENTITY_CLASS,
                                        ClassName.get(type)
                                )
                        )
                )
                .beginControlFlow("if (response.isPresent())")
                .addStatement("return response.get()")
                .endControlFlow()
                .endControlFlow()
                .addStatement("return $T.defaultGetById(id, service)", overrideType);

        if (policy != null && !policy.isBlank()) {
            methodSpec.addAnnotation(generatePreAuthorizeAnnotation(policy));
        }

        return methodSpec.build();
    }

    private static MethodSpec generatePostMethod(
            TypeElement type,
            String simpleName,
            String policy,
            ClassName overrideType
    ) {
        var methodSpec = MethodSpec.methodBuilder("create")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(AnnotationSpec
                        .builder(ClassName.get(SPRING_WEB_ANNOTATION_PACKAGE, "PostMapping"))
                        .addMember("consumes", "$S", APPLICATION_JSON)
                        .build()
                )
                .addParameter(ParameterSpec.builder(ClassName.get(type), "new" + simpleName)
                        .addAnnotation(ClassName.get(SPRING_WEB_ANNOTATION_PACKAGE, "RequestBody"))
                        .addAnnotation(ClassName.get("jakarta.validation", "Valid"))
                        .build()
                )
                .returns(ParameterizedTypeName.get(
                        RESPONSE_ENTITY_CLASS,
                        ClassName.get(type)
                ))
                .addJavadoc("Creates a new $L item.\n", simpleName)
                .beginControlFlow("for (var override : overrides)")
                .addStatement("$T response = override.create(new$L, service)",
                        ParameterizedTypeName.get(
                                OPTIONAL_CLASS,
                                ParameterizedTypeName.get(
                                        RESPONSE_ENTITY_CLASS,
                                        ClassName.get(type)
                                )
                        ),
                        simpleName
                )
                .beginControlFlow("if (response.isPresent())")
                .addStatement("return response.get()")
                .endControlFlow()
                .endControlFlow()
                .addStatement("return $T.defaultCreate(new$L, service)", overrideType, simpleName);

        if (policy != null && !policy.isBlank()) {
            methodSpec.addAnnotation(generatePreAuthorizeAnnotation(policy));
        }

        return methodSpec.build();
    }

    private static MethodSpec generatePutMethod(
            TypeElement type,
            String simpleName,
            TypeMirror idType,
            String policy,
            ClassName overrideType
    ) {
        var methodSpec = MethodSpec.methodBuilder("update")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(AnnotationSpec
                        .builder(ClassName.get(SPRING_WEB_ANNOTATION_PACKAGE, "PutMapping"))
                        .addMember("path", "$S", ID)
                        .addMember("consumes", "$S", APPLICATION_JSON)
                        .build()
                )
                .addParameter(ParameterSpec.builder(TypeName.get(idType), "id")
                        .addAnnotation(AnnotationSpec
                                .builder(PATH_VARIABLE_CLASS)
                                .addMember(VALUE, "$S", "id").build()
                        ).build()
                )
                .addParameter(ParameterSpec.builder(ClassName.get(type), "updated" + simpleName)
                        .addAnnotation(ClassName.get(SPRING_WEB_ANNOTATION_PACKAGE, "RequestBody"))
                        .addAnnotation(ClassName.get("jakarta.validation", "Valid"))
                        .build()
                )
                .returns(ParameterizedTypeName.get(
                        RESPONSE_ENTITY_CLASS,
                        ClassName.get(type)
                ))
                .addJavadoc("Updates an existing $L item by its ID.\n", simpleName)
                .beginControlFlow("for (var override : overrides)")
                .addStatement("$T response = override.update(id, updated$L, service)",
                        ParameterizedTypeName.get(
                                OPTIONAL_CLASS,
                                ParameterizedTypeName.get(
                                        RESPONSE_ENTITY_CLASS,
                                        ClassName.get(type)
                                )
                        ),
                        simpleName
                )
                .beginControlFlow("if (response.isPresent())")
                .addStatement("return response.get()")
                .endControlFlow()
                .endControlFlow()
                .addStatement("return $T.defaultUpdate(id, updated$L, service)", overrideType, simpleName);

        if (policy != null && !policy.isBlank()) {
            methodSpec.addAnnotation(generatePreAuthorizeAnnotation(policy));
        }

        return methodSpec.build();
    }

    private static MethodSpec generateDeleteMethod(
            String simpleName,
            TypeMirror idType,
            String policy,
            ClassName overrideType
    ) {
        var methodSpec = MethodSpec.methodBuilder("deleteById")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(AnnotationSpec
                        .builder(ClassName.get(SPRING_WEB_ANNOTATION_PACKAGE, "DeleteMapping"))
                        .addMember("path", "$S", ID)
                        .build()
                )
                .addParameter(ParameterSpec.builder(TypeName.get(idType), "id")
                        .addAnnotation(AnnotationSpec
                                .builder(PATH_VARIABLE_CLASS)
                                .addMember(VALUE, "$S", "id").build()
                        ).build()
                )
                .returns(ParameterizedTypeName.get(
                        RESPONSE_ENTITY_CLASS,
                        TypeName.VOID.box()
                ))
                .addJavadoc("Deletes an existing $L item by its ID.\n", simpleName)
                .beginControlFlow("for (var override : overrides)")
                .addStatement("$T response = override.deleteById(id, service)",
                        ParameterizedTypeName.get(
                                OPTIONAL_CLASS,
                                ParameterizedTypeName.get(
                                        RESPONSE_ENTITY_CLASS,
                                        TypeName.VOID.box()
                                )
                        )
                )
                .beginControlFlow("if (response.isPresent())")
                .addStatement("return response.get()")
                .endControlFlow()
                .endControlFlow()
                .addStatement("return $T.defaultDeleteById(id, service)", overrideType);

        if (policy != null && !policy.isBlank()) {
            methodSpec.addAnnotation(generatePreAuthorizeAnnotation(policy));
        }

        return methodSpec.build();
    }

    private static AnnotationSpec generatePreAuthorizeAnnotation(String policy) {
        return AnnotationSpec
                .builder(ClassName.get("org.springframework.security.access.prepost", "PreAuthorize"))
                .addMember(VALUE, "$S", policy)
                .build();
    }
}
