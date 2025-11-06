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
import de.flix29.sprout.annotations.model.Endpoint;

import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import java.util.Arrays;

public class SproutControllerProcessor {

    private static final String SPRING_WEB_ANNOTATION_PACKAGE = "org.springframework.web.bind.annotation";
    private static final String SWAGGER_API_RESPONSE_ANNOTATION = "io.swagger.v3.oas.annotations.responses.ApiResponse";

    private static final ClassName PATH_VARIABLE_CLASS = ClassName.get(SPRING_WEB_ANNOTATION_PACKAGE, "PathVariable");
    private static final ClassName RESPONSE_ENTITY_CLASS = ClassName.get("org.springframework.http", "ResponseEntity");
    private static final ClassName LIST_CLASS = ClassName.get("java.util", "List");
    private static final ClassName API_RESPONSES =
            ClassName.get("io.swagger.v3.oas.annotations.responses", "ApiResponses");

    private static final String SWAGGER_OK =
            generateSwaggerApiResponseString(200, "Successful retrieval");
    private static final String SWAGGER_CREATED =
            generateSwaggerApiResponseString(201, "Successful created");
    private static final String SWAGGER_NO_CONTENT =
            generateSwaggerApiResponseString(204, "Successful deleted");
    private static final String SWAGGER_BAD_REQUEST =
            generateSwaggerApiResponseString(400, "Invalid input data");
    private static final String SWAGGER_UNAUTHORIZED =
            generateSwaggerApiResponseString(401, "Unauthorized");
    private static final String SWAGGER_ACCESS_DENIED =
            generateSwaggerApiResponseString(403, "Access denied");
    private static final String SWAGGER_NOT_FOUND =
            generateSwaggerApiResponseString(404, "Not found");
    private static final String SWAGGER_INTERNAL_SERVER_ERROR =
            generateSwaggerApiResponseString(500, "Internal server error");

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
            boolean swaggerNeeded,
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
                );

        if (sproutResource.generateSwaggerDocs() && swaggerNeeded) {
            typeSpec.addAnnotation(AnnotationSpec
                    .builder(ClassName.get("io.swagger.v3.oas.annotations.tags", "Tag"))
                    .addMember("name", "$S", getTagName(sproutResource, simpleName))
                    .addMember("description", "$S", sproutResource.summary())
                    .build()
            );
        }

        if (methodGenerationAllowed(Endpoint.GET_ALL, sproutResource)) {
            typeSpec.addMethod(generateGetAllMethod(
                    type,
                    simpleName,
                    policy == null ? null : policy.read(),
                    sproutResource.generateSwaggerDocs() && swaggerNeeded
            ));
        }

        if (methodGenerationAllowed(Endpoint.GET_BY_ID, sproutResource)) {
            typeSpec.addMethod(generateGetByIdMethod(
                    type,
                    simpleName,
                    idType,
                    policy == null ? null : policy.read(),
                    sproutResource.generateSwaggerDocs() && swaggerNeeded
            ));
        }

        if (!sproutResource.readOnly()) {
            if (methodGenerationAllowed(Endpoint.CREATE, sproutResource)) {
                typeSpec.addMethod(generatePostMethod(
                        type,
                        simpleName,
                        policy == null ? null : policy.create(),
                        sproutResource.generateSwaggerDocs() && swaggerNeeded
                ));
            }
            if (methodGenerationAllowed(Endpoint.UPDATE, sproutResource)) {
                typeSpec.addMethod(generatePutMethod(
                        type,
                        simpleName,
                        idType,
                        policy == null ? null : policy.update(),
                        sproutResource.generateSwaggerDocs() && swaggerNeeded
                ));
            }
            if (methodGenerationAllowed(Endpoint.DELETE, sproutResource)) {
                typeSpec.addMethod(generateDeleteMethod(
                        simpleName,
                        idType,
                        policy == null ? null : policy.delete(),
                        sproutResource.generateSwaggerDocs() && swaggerNeeded
                ));
            }
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
            addSwaggerApiResponsesAnnotation(
                    methodSpec,
                    SWAGGER_OK,
                    SWAGGER_UNAUTHORIZED,
                    SWAGGER_ACCESS_DENIED,
                    SWAGGER_INTERNAL_SERVER_ERROR
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
            addSwaggerApiResponsesAnnotation(
                    methodSpec,
                    SWAGGER_OK,
                    SWAGGER_UNAUTHORIZED,
                    SWAGGER_ACCESS_DENIED,
                    SWAGGER_NOT_FOUND,
                    SWAGGER_INTERNAL_SERVER_ERROR
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
            addSwaggerApiResponsesAnnotation(
                    methodSpec,
                    SWAGGER_CREATED,
                    SWAGGER_BAD_REQUEST,
                    SWAGGER_UNAUTHORIZED,
                    SWAGGER_ACCESS_DENIED,
                    SWAGGER_INTERNAL_SERVER_ERROR
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
            addSwaggerApiResponsesAnnotation(
                    methodSpec,
                    SWAGGER_OK,
                    SWAGGER_BAD_REQUEST,
                    SWAGGER_UNAUTHORIZED,
                    SWAGGER_ACCESS_DENIED,
                    SWAGGER_NOT_FOUND,
                    SWAGGER_INTERNAL_SERVER_ERROR
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
            addSwaggerApiResponsesAnnotation(
                    methodSpec,
                    SWAGGER_NO_CONTENT,
                    SWAGGER_UNAUTHORIZED,
                    SWAGGER_ACCESS_DENIED,
                    SWAGGER_NOT_FOUND,
                    SWAGGER_INTERNAL_SERVER_ERROR
            );
        }

        if (policy != null && !policy.isBlank()) {
            methodSpec.addAnnotation(generatePreAuthorizeAnnotation(policy));
        }

        return methodSpec.build();
    }

    private static boolean methodGenerationAllowed(Endpoint endpoint, SproutResource sproutResource) {
        var excluded = Arrays.asList(sproutResource.exclude());
        if (excluded.contains(endpoint)) {
            return false;
        }
        if (sproutResource.include().length == 0) {
            return true;
        }
        if (sproutResource.readOnly() &&
                (endpoint == Endpoint.GET_ALL && !excluded.contains(Endpoint.GET_ALL)
                        || endpoint == Endpoint.GET_BY_ID && !excluded.contains(Endpoint.GET_BY_ID))
        ) {
            return true;
        }
        return Arrays.asList(sproutResource.include()).contains(endpoint);
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

    private static String generateSwaggerApiResponseString(int responseCode, String description) {
        return "@%s(responseCode = \"%d\", description = \"%s\")".formatted(
                SWAGGER_API_RESPONSE_ANNOTATION,
                responseCode,
                description
        );

    }

    private static void addSwaggerApiResponsesAnnotation(
            MethodSpec.Builder methodSpec,
            String... apiResponses
    ) {
        var annotationSpec = AnnotationSpec.builder(API_RESPONSES);
        Arrays.stream(apiResponses).forEach(apiResponse ->
                annotationSpec.addMember(VALUE, "$L", apiResponse)
        );
        methodSpec.addAnnotation(annotationSpec.build());
    }
}
