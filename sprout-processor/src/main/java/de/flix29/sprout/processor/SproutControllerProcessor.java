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
    private static final ClassName CONDITIONAL_ON_MISSING_BEAN = ClassName.get(
            "org.springframework.boot.autoconfigure.condition",
            "ConditionalOnMissingBean"
    );
    private static final ClassName LIST_CLASS = ClassName.get("java.util", "List");
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
        ClassName operations = ClassName.get(basePackage + ".services", componentName + "Operations");
        ClassName controllerType = ClassName.get(basePackage + ".controllers", componentName + "Controller");
        var typeSpec = TypeSpec.classBuilder(componentName + "Controller")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(ClassName.get(SPRING_WEB_ANNOTATION_PACKAGE, "RestController"))
                .addAnnotation(AnnotationSpec
                        .builder(ClassName.get(SPRING_WEB_ANNOTATION_PACKAGE, "RequestMapping"))
                        .addMember("path", "$S", apiPath)
                        .addMember("produces", "$S", APPLICATION_JSON)
                        .build()
                )
                .addAnnotation(AnnotationSpec.builder(CONDITIONAL_ON_MISSING_BEAN)
                        .addMember("value", "$T.class", controllerType)
                        .build()
                )
                .addField(FieldSpec.builder(
                                operations, "operations",
                                Modifier.PRIVATE, Modifier.FINAL
                        ).build()
                )
                .addMethod(MethodSpec.constructorBuilder()
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(operations, "operations")
                        .addStatement("this.operations = operations")
                        .build()
                )
                .addMethod(generateGetAllMethod(type, simpleName, policy == null ? null : policy.read()))
                .addMethod(generateGetByIdMethod(type, simpleName, idType, policy == null ? null : policy.read()));

        if (!readOnly) {
            typeSpec
                    .addMethod(generatePostMethod(type, simpleName, policy == null ? null : policy.create()))
                    .addMethod(generatePutMethod(type, simpleName, idType, policy == null ? null : policy.update()))
                    .addMethod(generateDeleteMethod(simpleName, idType, policy == null ? null : policy.delete()));
        }

        return typeSpec;
    }

    private static MethodSpec generateGetAllMethod(TypeElement type, String simpleName, String policy) {
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
                .addStatement("return $T.ok(operations.findAll())", RESPONSE_ENTITY_CLASS);

        if (policy != null && !policy.isBlank()) {
            methodSpec.addAnnotation(generatePreAuthorizeAnnotation(policy));
        }

        return methodSpec.build();
    }

    private static MethodSpec generateGetByIdMethod(
            TypeElement type, String simpleName, TypeMirror idType, String policy
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
                .addStatement("""
                                return operations.findById(id)
                                        .map($T::ok)
                                        .orElse($T.notFound().build())
                                """,
                        RESPONSE_ENTITY_CLASS, RESPONSE_ENTITY_CLASS
                );

        if (policy != null && !policy.isBlank()) {
            methodSpec.addAnnotation(generatePreAuthorizeAnnotation(policy));
        }

        return methodSpec.build();
    }

    private static MethodSpec generatePostMethod(TypeElement type, String simpleName, String policy) {
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
                .addStatement("return $T.status($T.CREATED).body(operations.save(new$L))",
                        RESPONSE_ENTITY_CLASS, ClassName.get("org.springframework.http", "HttpStatus"), simpleName
                );

        if (policy != null && !policy.isBlank()) {
            methodSpec.addAnnotation(generatePreAuthorizeAnnotation(policy));
        }

        return methodSpec.build();
    }

    private static MethodSpec generatePutMethod(TypeElement type, String simpleName, TypeMirror idType, String policy) {
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
                .addStatement("""
                                return operations.update(id, updated$L)
                                    .map($T::ok)
                                    .orElse($T.notFound().build())
                                """,
                        simpleName, RESPONSE_ENTITY_CLASS, RESPONSE_ENTITY_CLASS
                );

        if (policy != null && !policy.isBlank()) {
            methodSpec.addAnnotation(generatePreAuthorizeAnnotation(policy));
        }

        return methodSpec.build();
    }

    private static MethodSpec generateDeleteMethod(String simpleName, TypeMirror idType, String policy) {
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
                .addStatement("return operations.deleteById(id) ? $T.noContent().build() : $T.notFound().build()",
                        RESPONSE_ENTITY_CLASS, RESPONSE_ENTITY_CLASS
                );

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
