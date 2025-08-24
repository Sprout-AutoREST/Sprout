package de.flix29.processor;

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

public class SproutControllerProcessor {

    private static final String SPRING_WEB_ANNOTATION_PACKAGE = "org.springframework.web.bind.annotation";
    private static final ClassName RESPONSE_ENTITY_CLASS = ClassName.get("org.springframework.http", "ResponseEntity");
    private static final ClassName LIST_CLASS = ClassName.get("java.util", "List");

    private SproutControllerProcessor() {
        // Utility class
    }

    protected static TypeSpec.Builder generateController(
            TypeElement type, String simpleName, String basePackage, boolean readOnly, String apiPath, TypeMirror idType
    ) {
        final String componentName = "Sprout" + simpleName;
        return TypeSpec.classBuilder(componentName + "Controller")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(ClassName.get(SPRING_WEB_ANNOTATION_PACKAGE, "RestController"))
                .addAnnotation(AnnotationSpec
                        .builder(ClassName.get(SPRING_WEB_ANNOTATION_PACKAGE, "RequestMapping"))
                        .addMember("path", "$S", apiPath)
                        .addMember("produces", "$S", "application/json")
                        .build()
                )
                .addField(FieldSpec.builder(
                                ClassName.get(basePackage, componentName + "Service"), "service",
                                Modifier.PRIVATE, Modifier.FINAL
                        ).build()
                )
                .addMethod(MethodSpec.constructorBuilder()
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(ClassName.get(basePackage, componentName + "Service"), "service")
                        .addStatement("this.service = service")
                        .build()
                )
                .addMethod(generateGetAllMethod(type, simpleName))
                .addMethod(generateGetByIdMethod(type, simpleName, idType))
                ;
    }

    private static MethodSpec generateGetAllMethod(TypeElement type, String simpleName) {
        return MethodSpec.methodBuilder("listAll")
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
                .addStatement("return $T.ok(service.findAll())", RESPONSE_ENTITY_CLASS)
                .build();
    }

    private static MethodSpec generateGetByIdMethod(TypeElement type, String simpleName, TypeMirror idType) {
        return MethodSpec.methodBuilder("getById")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(AnnotationSpec
                        .builder(ClassName.get(SPRING_WEB_ANNOTATION_PACKAGE, "GetMapping"))
                        .addMember("path", "$S", "/{id}")
                        .build()
                )
                .addParameter(TypeName.get(idType), "id")
                .returns(ParameterizedTypeName.get(
                        RESPONSE_ENTITY_CLASS,
                        ClassName.get(type)
                ))
                .addJavadoc("Returns a single $L item by its ID.\n", simpleName)
                .addStatement("""
                        return service.findById(id)
                                .map($T::ok)
                                .orElse($T.notFound().build())
                        """,
                        RESPONSE_ENTITY_CLASS, RESPONSE_ENTITY_CLASS
                )
                .build();
    }
}
