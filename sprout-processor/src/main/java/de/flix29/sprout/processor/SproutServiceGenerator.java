package de.flix29.sprout.processor;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
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

public class SproutServiceGenerator {

    private static final ClassName LIST = ClassName.get("java.util", "List");
    private static final ClassName OPTIONAL = ClassName.get("java.util", "Optional");
    private static final ClassName OVERRIDE = ClassName.get(Override.class);
    private static final ClassName BEAN_UTILS = ClassName.get("org.springframework.beans", "BeanUtils");
    private static final ClassName SERVICE = ClassName.get("org.springframework.stereotype", "Service");
    private static final ClassName TRANSACTIONAL =
            ClassName.get("org.springframework.transaction.annotation", "Transactional");
    private static final ClassName AUTHENTICATION =
            ClassName.get("org.springframework.security.core", "Authentication");
    private static final String AUTHENTICATION_PARAM = "authentication";

    private SproutServiceGenerator() {
        // Utility class
    }

    protected static TypeSpec.Builder generateService(
            TypeElement type, String simpleName, String basePath, SproutResource sproutResource, TypeMirror idType
    ) {
        final String componentName = "Sprout" + simpleName;
        final ClassName repository = ClassName.get(basePath + ".repositories", componentName + "Repository");
        final ClassName operations = ClassName.get(basePath + ".services", componentName + "Operations");
        final ClassName entityType = ClassName.get(type);
        final TypeName idT = TypeName.get(idType);

        var builder = TypeSpec.classBuilder(componentName + "Service")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(SERVICE)
                .addSuperinterface(operations)
                .addField(FieldSpec.builder(
                        repository, "repository", Modifier.PROTECTED, Modifier.FINAL
                ).build())
                .addMethod(MethodSpec.constructorBuilder()
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(repository, "repository")
                        .addStatement("this.repository = repository")
                        .build()
                );

        if (sproutResource.readOnly()) {
            builder.addAnnotation(AnnotationSpec.builder(TRANSACTIONAL)
                    .addMember("readOnly", "$L", true)
                    .build()
            );
        }

        if (methodGenerationAllowed(Endpoint.GET_ALL, sproutResource)) {
            builder.addMethod(generateFindAllMethod(entityType, sproutResource.authenticationPrincipal()));
        }

        if (methodGenerationAllowed(Endpoint.GET_BY_ID, sproutResource)) {
            builder.addMethod(generateFindByIdMethod(entityType, idT, sproutResource.authenticationPrincipal()));
        }

        if (!sproutResource.readOnly()) {
            if (methodGenerationAllowed(Endpoint.CREATE, sproutResource)) {
                builder.addMethod(generateSaveMethod(entityType, sproutResource.authenticationPrincipal()));
            }
            if (methodGenerationAllowed(Endpoint.UPDATE, sproutResource)) {
                builder.addMethod(generateUpdateMethod(entityType, idT, sproutResource.authenticationPrincipal()));
            }
            if (methodGenerationAllowed(Endpoint.DELETE, sproutResource)) {
                builder.addMethod(generateDeleteMethod(idT, sproutResource.authenticationPrincipal()));
            }
        }

        return builder;
    }

    private static MethodSpec generateFindAllMethod(ClassName entityType, boolean authentication) {
        var builder = MethodSpec.methodBuilder("findAll")
                .addAnnotation(OVERRIDE)
                .addModifiers(Modifier.PUBLIC)
                .returns(ParameterizedTypeName.get(LIST, entityType))
                .addStatement("return repository.findAll()");

        if (authentication) {
            builder.addParameter(AUTHENTICATION, AUTHENTICATION_PARAM);
        }

        return builder.build();
    }

    private static MethodSpec generateFindByIdMethod(ClassName entityType, TypeName idT, boolean authentication) {
        var builder = MethodSpec.methodBuilder("findById")
                .addAnnotation(OVERRIDE)
                .addModifiers(Modifier.PUBLIC)
                .addParameter(idT, "id")
                .returns(ParameterizedTypeName.get(OPTIONAL, entityType))
                .addStatement("return repository.findById(id)");

        if (authentication) {
            builder.addParameter(AUTHENTICATION, AUTHENTICATION_PARAM);
        }

        return builder.build();
    }

    private static MethodSpec generateSaveMethod(ClassName entityType, boolean authentication) {
        var builder = MethodSpec.methodBuilder("save")
                .addAnnotation(TRANSACTIONAL)
                .addAnnotation(OVERRIDE)
                .addModifiers(Modifier.PUBLIC)
                .addParameter(entityType, "entity")
                .returns(entityType)
                .addStatement("return repository.save(entity)");

        if (authentication) {
            builder.addParameter(AUTHENTICATION, AUTHENTICATION_PARAM);
        }

        return builder.build();
    }

    private static MethodSpec generateUpdateMethod(ClassName entityType, TypeName idT, boolean authentication) {
        var builder = MethodSpec.methodBuilder("update")
                .addAnnotation(TRANSACTIONAL)
                .addAnnotation(OVERRIDE)
                .addModifiers(Modifier.PUBLIC)
                .addParameter(idT, "id")
                .addParameter(entityType, "entity")
                .returns(ParameterizedTypeName.get(OPTIONAL, entityType))
                .addStatement("return repository.findById(id).map(existing -> { " +
                        "$T.copyProperties(entity, existing, $S); " +
                        "return repository.save(existing); " +
                        "})", BEAN_UTILS, "id");

        if (authentication) {
            builder.addParameter(AUTHENTICATION, AUTHENTICATION_PARAM);
        }

        return builder.build();
    }

    private static MethodSpec generateDeleteMethod(TypeName idT, boolean authentication) {
        var builder = MethodSpec.methodBuilder("deleteById")
                .addAnnotation(TRANSACTIONAL)
                .addAnnotation(OVERRIDE)
                .addModifiers(Modifier.PUBLIC)
                .addParameter(idT, "id")
                .returns(TypeName.BOOLEAN)
                .beginControlFlow("if (!repository.existsById(id))")
                .addStatement("return false")
                .endControlFlow()
                .addStatement("repository.deleteById(id)")
                .addStatement("return true");

        if (authentication) {
            builder.addParameter(AUTHENTICATION, AUTHENTICATION_PARAM);
        }

        return builder.build();
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
