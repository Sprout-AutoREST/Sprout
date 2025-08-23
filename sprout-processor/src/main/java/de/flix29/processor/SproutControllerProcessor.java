package de.flix29.processor;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;

import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;

public class SproutControllerProcessor {

    private static final String SPRING_WEB_ANNOTATION_PACKAGE = "org.springframework.web.bind.annotation";

    private SproutControllerProcessor() {
        // Utility class
    }

    protected static TypeSpec.Builder generateController(
            TypeElement type, String simpleName, String basePackage, boolean readOnly, String apiPath, TypeMirror idType
    ) {
        return TypeSpec.classBuilder("Sprout" + simpleName + "Controller")
                .addModifiers(javax.lang.model.element.Modifier.PUBLIC)
                .addAnnotation(ClassName.get(SPRING_WEB_ANNOTATION_PACKAGE, "RestController"))
                .addAnnotation(AnnotationSpec
                        .builder(ClassName.get(SPRING_WEB_ANNOTATION_PACKAGE, "RequestMapping"))
                        .addMember("path", "$S", apiPath)
                        .build()
                )
                .addAnnotation(ClassName.get("lombok", "RequiredArgsConstructor"))
                .addField(FieldSpec.builder(
                                ClassName.get(basePackage, "Sprout" + simpleName + "Service"), "service",
                                Modifier.PRIVATE, Modifier.FINAL
                        ).build()
                )
                .addMethod(generateGetAllMethod(type, simpleName))
                ;
    }

    private static MethodSpec generateGetAllMethod(TypeElement type, String simpleName) {
        return MethodSpec.methodBuilder("getAll" + simpleName + "s")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(AnnotationSpec
                        .builder(ClassName.get(SPRING_WEB_ANNOTATION_PACKAGE, "GetMapping"))
                        .build()
                )
                .returns(ParameterizedTypeName.get(
                        ClassName.get("org.springframework.http", "ResponseEntity"),
                        ParameterizedTypeName.get(
                                ClassName.get("java.util", "List"),
                                ClassName.get(type)
                        )
                ))
                .addStatement("var response = service.getAll()")
                .addStatement("return ResponseEntity.ok(response)")
                .build();
    }
}
