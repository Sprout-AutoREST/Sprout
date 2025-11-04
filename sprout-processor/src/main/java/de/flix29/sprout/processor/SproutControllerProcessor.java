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
import de.flix29.sprout.annotations.SproutResource;

import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;

public class SproutControllerProcessor {

    private static final String SPRING_WEB_ANNOTATION_PACKAGE = "org.springframework.web.bind.annotation";
    private static final String SWAGGER_API_RESPONSE_ANNOTATION = "io.swagger.v3.oas.annotations.responses.ApiResponse";

    private static final ClassName PATH_VARIABLE_CLASS = ClassName.get(SPRING_WEB_ANNOTATION_PACKAGE, "PathVariable");
    private static final ClassName RESPONSE_ENTITY_CLASS = ClassName.get("org.springframework.http", "ResponseEntity");
    private static final ClassName LIST_CLASS = ClassName.get("java.util", "List");
    private static final ClassName API_RESPONSES = ClassName.get("io.swagger.v3.oas.annotations.responses", "ApiResponses");

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
            SproutResource sproutResource,
            SproutPolicy policy,
            String apiPath,
            TypeMirror idType
    ) {
        final String componentName = "Sprout" + simpleName;
        ClassName operations = ClassName.get(basePackage + ".services", componentName + "Operations");
        var typeSpec = TypeSpec.classBuilder(componentName + "Controller")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(ClassName.get(SPRING_WEB_ANNOTATION_PACKAGE, "RestController"))
                .addAnnotation(AnnotationSpec
                        .builder(ClassName.get(SPRING_WEB_ANNOTATION_PACKAGE, "RequestMapping"))
                        .addMember("path", "$S", apiPath)
                        .addMember("produces", "$S", APPLICATION_JSON)
                        .build()
                ).addField(FieldSpec
                        .builder(operations, "operations", Modifier.PRIVATE, Modifier.FINAL)
                        .build()
                ).addMethod(MethodSpec.constructorBuilder()
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(operations, "operations")
                        .addStatement("this.operations = operations")
                        .build()
                ).addMethod(generateGetAllMethod(
                        type,
                        simpleName,
                        policy == null ? null : policy.read(),
                        sproutResource.generateSwaggerDocs()
                )).addMethod(generateGetByIdMethod(
                        type,
                        simpleName,
                        idType,
                        policy == null ? null : policy.read(),
                        sproutResource.generateSwaggerDocs()
                ));

        if (sproutResource.generateSwaggerDocs()) {
            typeSpec.addAnnotation(AnnotationSpec
                    .builder(ClassName.get("io.swagger.v3.oas.annotations.tags", "Tag"))
                    .addMember("name", "$S", getTagName(sproutResource, simpleName))
                    .addMember("description", "$S", sproutResource.summary())
                    .build()
            );
        }

        if (!sproutResource.readOnly()) {
            typeSpec.addMethod(generatePostMethod(
                    type,
                    simpleName,
                    policy == null ? null : policy.create(),
                    sproutResource.generateSwaggerDocs()
            )).addMethod(generatePutMethod(
                    type,
                    simpleName,
                    idType,
                    policy == null ? null : policy.update(),
                    sproutResource.generateSwaggerDocs()
            )).addMethod(generateDeleteMethod(
                    simpleName,
                    idType,
                    policy == null ? null : policy.delete(),
                    sproutResource.generateSwaggerDocs()
            ));
        }

        return typeSpec;
    }

    private static MethodSpec generateGetAllMethod(
            TypeElement type, String simpleName, String policy, boolean generateSwaggerDocs
    ) {
        var methodSpec = MethodSpec.methodBuilder("getAll")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(AnnotationSpec
                        .builder(ClassName.get(SPRING_WEB_ANNOTATION_PACKAGE, "GetMapping"))
                        .build()
                ).returns(ParameterizedTypeName.get(
                        RESPONSE_ENTITY_CLASS,
                        ParameterizedTypeName.get(
                                LIST_CLASS,
                                ClassName.get(type)
                        )
                )).addJavadoc("Returns all $L items.\n", simpleName)
                .addStatement("return $T.ok(operations.findAll())", RESPONSE_ENTITY_CLASS);

        if (generateSwaggerDocs) {
            methodSpec.addAnnotation(AnnotationSpec
                    .builder(API_RESPONSES)
                    .addMember(VALUE, "$L", """
                            {
                                @%s(responseCode = "200", description = "Successful retrieval of %s items"),
                                @%s(responseCode = "401", description = "Unauthorized"),
                                @%s(responseCode = "403", description = "Access denied"),
                                @%s(responseCode = "500", description = "Internal server error")
                            }
                            """.formatted(
                                    SWAGGER_API_RESPONSE_ANNOTATION,
                                    simpleName,
                                    SWAGGER_API_RESPONSE_ANNOTATION,
                                    SWAGGER_API_RESPONSE_ANNOTATION,
                                    SWAGGER_API_RESPONSE_ANNOTATION
                            )
                    ).build()
            );
        }

        if (policy != null && !policy.isBlank()) {
            methodSpec.addAnnotation(generatePreAuthorizeAnnotation(policy));
        }

        return methodSpec.build();
    }

    private static MethodSpec generateGetByIdMethod(
            TypeElement type, String simpleName, TypeMirror idType, String policy, boolean generateSwaggerDocs
    ) {
        var methodSpec = MethodSpec.methodBuilder("getById")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(AnnotationSpec
                        .builder(ClassName.get(SPRING_WEB_ANNOTATION_PACKAGE, "GetMapping"))
                        .addMember("path", "$S", ID)
                        .build()
                ).addParameter(ParameterSpec.builder(TypeName.get(idType), "id")
                        .addAnnotation(PATH_VARIABLE_CLASS)
                        .build()
                ).returns(ParameterizedTypeName.get(
                        RESPONSE_ENTITY_CLASS,
                        ClassName.get(type)
                )).addJavadoc("Returns a single $L item by its ID.\n", simpleName)
                .addStatement("""
                                return operations.findById(id)
                                        .map($T::ok)
                                        .orElse($T.notFound().build())
                                """,
                        RESPONSE_ENTITY_CLASS, RESPONSE_ENTITY_CLASS
                );

        if (generateSwaggerDocs) {
            methodSpec.addAnnotation(AnnotationSpec
                    .builder(API_RESPONSES)
                    .addMember(VALUE, "$L", """
                            {
                                @%s(responseCode = "200", description = "Successful retrieval of %s"),
                                @%s(responseCode = "401", description = "Unauthorized"),
                                @%s(responseCode = "403", description = "Access denied"),
                                @%s(responseCode = "404", description = "%s not found"),
                                @%s(responseCode = "500", description = "Internal server error")
                            }
                            """.formatted(
                                    SWAGGER_API_RESPONSE_ANNOTATION,
                                    simpleName,
                                    SWAGGER_API_RESPONSE_ANNOTATION,
                                    SWAGGER_API_RESPONSE_ANNOTATION,
                                    SWAGGER_API_RESPONSE_ANNOTATION,
                                    simpleName,
                                    SWAGGER_API_RESPONSE_ANNOTATION
                            )
                    ).build()
            );
        }

        if (policy != null && !policy.isBlank()) {
            methodSpec.addAnnotation(generatePreAuthorizeAnnotation(policy));
        }

        return methodSpec.build();
    }

    private static MethodSpec generatePostMethod(
            TypeElement type, String simpleName, String policy, boolean generateSwaggerDocs
    ) {
        var methodSpec = MethodSpec.methodBuilder("create")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(AnnotationSpec
                        .builder(ClassName.get(SPRING_WEB_ANNOTATION_PACKAGE, "PostMapping"))
                        .addMember("consumes", "$S", APPLICATION_JSON)
                        .build()
                ).addParameter(ParameterSpec.builder(ClassName.get(type), "new" + simpleName)
                        .addAnnotation(ClassName.get(SPRING_WEB_ANNOTATION_PACKAGE, "RequestBody"))
                        .addAnnotation(ClassName.get("jakarta.validation", "Valid"))
                        .build()
                ).returns(ParameterizedTypeName.get(
                        RESPONSE_ENTITY_CLASS,
                        ClassName.get(type)
                )).addJavadoc("Creates a new $L item.\n", simpleName)
                .addStatement("return $T.status($T.CREATED).body(operations.save(new$L))",
                        RESPONSE_ENTITY_CLASS, ClassName.get("org.springframework.http", "HttpStatus"), simpleName
                );

        if (generateSwaggerDocs) {
            methodSpec.addAnnotation(AnnotationSpec
                    .builder(API_RESPONSES)
                    .addMember(VALUE, "$L", """
                            {
                                @%s(responseCode = "201", description = "Successful created %s"),
                                @%s(responseCode = "400", description = "Invalid input data"),
                                @%s(responseCode = "401", description = "Unauthorized"),
                                @%s(responseCode = "403", description = "Access denied"),
                                @%s(responseCode = "500", description = "Internal server error")
                            }
                            """.formatted(
                                    SWAGGER_API_RESPONSE_ANNOTATION,
                                    simpleName,
                                    SWAGGER_API_RESPONSE_ANNOTATION,
                                    SWAGGER_API_RESPONSE_ANNOTATION,
                                    SWAGGER_API_RESPONSE_ANNOTATION,
                                    SWAGGER_API_RESPONSE_ANNOTATION
                            )
                    ).build()
            );
        }

        if (policy != null && !policy.isBlank()) {
            methodSpec.addAnnotation(generatePreAuthorizeAnnotation(policy));
        }

        return methodSpec.build();
    }

    private static MethodSpec generatePutMethod(
            TypeElement type, String simpleName, TypeMirror idType, String policy, boolean generateSwaggerDocs
    ) {
        var methodSpec = MethodSpec.methodBuilder("update")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(AnnotationSpec
                        .builder(ClassName.get(SPRING_WEB_ANNOTATION_PACKAGE, "PutMapping"))
                        .addMember("path", "$S", ID)
                        .addMember("consumes", "$S", APPLICATION_JSON)
                        .build()
                ).addParameter(ParameterSpec.builder(TypeName.get(idType), "id")
                        .addAnnotation(AnnotationSpec
                                .builder(PATH_VARIABLE_CLASS)
                                .addMember(VALUE, "$S", "id").build()
                        ).build()
                ).addParameter(ParameterSpec.builder(ClassName.get(type), "updated" + simpleName)
                        .addAnnotation(ClassName.get(SPRING_WEB_ANNOTATION_PACKAGE, "RequestBody"))
                        .addAnnotation(ClassName.get("jakarta.validation", "Valid"))
                        .build()
                ).returns(ParameterizedTypeName.get(
                        RESPONSE_ENTITY_CLASS,
                        ClassName.get(type)
                )).addJavadoc("Updates an existing $L item by its ID.\n", simpleName)
                .addStatement("""
                                return operations.update(id, updated$L)
                                    .map($T::ok)
                                    .orElse($T.notFound().build())
                                """,
                        simpleName, RESPONSE_ENTITY_CLASS, RESPONSE_ENTITY_CLASS
                );

        if (generateSwaggerDocs) {
            methodSpec.addAnnotation(AnnotationSpec
                    .builder(API_RESPONSES)
                    .addMember(VALUE, "$L", """
                            {
                                @%s(responseCode = "200", description = "Successful updated %s"),
                                @%s(responseCode = "400", description = "Invalid input data"),
                                @%s(responseCode = "401", description = "Unauthorized"),
                                @%s(responseCode = "403", description = "Access denied"),
                                @%s(responseCode = "404", description = "%s not found"),
                                @%s(responseCode = "500", description = "Internal server error")
                            }
                            """.formatted(
                                    SWAGGER_API_RESPONSE_ANNOTATION,
                                    simpleName,
                                    SWAGGER_API_RESPONSE_ANNOTATION,
                                    SWAGGER_API_RESPONSE_ANNOTATION,
                                    SWAGGER_API_RESPONSE_ANNOTATION,
                                    SWAGGER_API_RESPONSE_ANNOTATION,
                                    simpleName,
                                    SWAGGER_API_RESPONSE_ANNOTATION
                            )
                    ).build()
            );
        }

        if (policy != null && !policy.isBlank()) {
            methodSpec.addAnnotation(generatePreAuthorizeAnnotation(policy));
        }

        return methodSpec.build();
    }

    private static MethodSpec generateDeleteMethod(
            String simpleName, TypeMirror idType, String policy, boolean generateSwaggerDocs
    ) {
        var methodSpec = MethodSpec.methodBuilder("deleteById")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(AnnotationSpec
                        .builder(ClassName.get(SPRING_WEB_ANNOTATION_PACKAGE, "DeleteMapping"))
                        .addMember("path", "$S", ID)
                        .build()
                ).addParameter(ParameterSpec.builder(TypeName.get(idType), "id")
                        .addAnnotation(AnnotationSpec
                                .builder(PATH_VARIABLE_CLASS)
                                .addMember(VALUE, "$S", "id").build()
                        ).build()
                ).returns(ParameterizedTypeName.get(
                        RESPONSE_ENTITY_CLASS,
                        TypeName.VOID.box()
                )).addJavadoc("Deletes an existing $L item by its ID.\n", simpleName)
                .addStatement("return operations.deleteById(id) ? $T.noContent().build() : $T.notFound().build()",
                        RESPONSE_ENTITY_CLASS, RESPONSE_ENTITY_CLASS
                );

        if (generateSwaggerDocs) {
            methodSpec.addAnnotation(AnnotationSpec
                    .builder(API_RESPONSES)
                    .addMember(VALUE, "$L", """
                            {
                                @%s(responseCode = "204", description = "Successful deleted %s"),
                                @%s(responseCode = "401", description = "Unauthorized"),
                                @%s(responseCode = "403", description = "Access denied"),
                                @%s(responseCode = "404", description = "%s not found"),
                                @%s(responseCode = "500", description = "Internal server error")
                            }
                            """.formatted(
                                    SWAGGER_API_RESPONSE_ANNOTATION,
                                    simpleName,
                                    SWAGGER_API_RESPONSE_ANNOTATION,
                                    SWAGGER_API_RESPONSE_ANNOTATION,
                                    SWAGGER_API_RESPONSE_ANNOTATION,
                                    simpleName,
                                    SWAGGER_API_RESPONSE_ANNOTATION
                            )
                    ).build()
            );
        }

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

    private static String getTagName(SproutResource sproutResource, String simpleName) {
        if (!sproutResource.tag().isBlank()) {
            return sproutResource.tag();
        } else if (!sproutResource.name().isBlank()) {
            return sproutResource.name();
        } else {
            return simpleName;
        }
    }
}
