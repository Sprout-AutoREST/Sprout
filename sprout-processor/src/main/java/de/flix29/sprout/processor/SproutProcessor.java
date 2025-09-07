package de.flix29.sprout.processor;

import com.google.auto.service.AutoService;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeSpec;
import de.flix29.sprout.annotations.SproutPolicy;
import de.flix29.sprout.annotations.SproutResource;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@AutoService(Processor.class)
@SupportedSourceVersion(SourceVersion.RELEASE_21)
public class SproutProcessor extends AbstractProcessor {

    private static final String SPROUT_ID = "de.flix29.sprout.annotations.SproutId";
    private static final String JAKARTA_ID = "jakarta.persistence.Id";
    private static final String JAVAX_ID = "javax.persistence.Id";

    private static final String JAKARTA_EMBEDDED_ID = "jakarta.persistence.EmbeddedId";
    private static final String JAVAX_EMBEDDED_ID = "javax.persistence.EmbeddedId";

    private static final List<String> ID_ORDER = List.of(SPROUT_ID, JAKARTA_ID, JAVAX_ID);

    private static final String JAKARTA_ENTITY = "jakarta.persistence.Entity";
    private static final String JAVAX_ENTITY = "javax.persistence.Entity";

    private static final String PRE_AUTHORIZE = "org.springframework.security.access.prepost.PreAuthorize";

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Set.of(SproutResource.class.getCanonicalName());
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        var classes = roundEnv.getElementsAnnotatedWith(SproutResource.class).stream()
                .filter(element -> element.getKind().isClass())
                .filter(TypeElement.class::isInstance)
                .map(TypeElement.class::cast)
                .toList();

        for (TypeElement type : classes) {
            SproutResource annotation = type.getAnnotation(SproutResource.class);
            SproutPolicy policyAnnotation = type.getAnnotation(SproutPolicy.class);

            String simpleName = annotation.name().isBlank() ? type.getSimpleName().toString() : annotation.name();
            String entityName = resolveJpaEntityName(type);
            if (entityName == null || entityName.isBlank()) {
                entityName = simpleName;
            }
            String derivedPath = "/api/" + simpleName.toLowerCase() + "s";
            String apiPath =
                    (annotation.path() != null && !annotation.path().isBlank()) ? annotation.path() : derivedPath;

            processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE,
                    "[Sprout] Generating marker for " + simpleName + " at apiPath " + apiPath);

            String basePackage = baseGeneratedPackage(type);
            String className = simpleName + "SproutMarker";

            Element idElement;
            try {
                idElement = findIdElement(type);
            } catch (IllegalStateException ex) {
                continue;
            }
            var idType = getIdTypeFromElement(idElement);
            var idName = getIdNameFromElement(idElement);

            var marker = SproutMarkerProcessor.generateMarker(
                    type.asType(),
                    className,
                    apiPath,
                    policyAnnotation,
                    entityName,
                    idName
            );
            var repository = SproutRepositoryGenerator.generateRepository(type, simpleName, entityName, idName, idType);
            var service = SproutServiceGenerator
                    .generateService(type, simpleName, basePackage, annotation.readOnly(), idType);
            var controller = SproutControllerProcessor.generateController(
                    type,
                    simpleName,
                    basePackage,
                    annotation.readOnly(),
                    hasClass(PRE_AUTHORIZE) ? policyAnnotation : null,
                    apiPath,
                    idType
            );

            processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE,
                    "[Sprout] ID for " + type.getSimpleName() + " -> " + idType);

            JavaFile markerFile = createJavaFile(basePackage + ".marker", marker);
            JavaFile repositoryFile = createJavaFile(basePackage + ".repositories", repository);
            JavaFile serviceFile = createJavaFile(basePackage + ".services", service);
            JavaFile controllerFile = createJavaFile(basePackage + ".controllers", controller);
            writeFiles(markerFile, repositoryFile, serviceFile, controllerFile);
        }
        return true;
    }

    private String baseGeneratedPackage(TypeElement type) {
        String entityPkg = processingEnv.getElementUtils().getPackageOf(type).getQualifiedName().toString();
        return entityPkg + ".generated";
    }

    private Element findIdElement(TypeElement type) throws IllegalStateException {
        if (hasEmbeddedId(type)) {
            processingEnv.getMessager().printMessage(
                    Diagnostic.Kind.ERROR,
                    "[Sprout] @EmbeddedId is not supported " + type.getQualifiedName()
            );
            throw new IllegalStateException("EmbeddedId is not supported in Sprout. Please use a single field or " +
                    "method annotated with @SproutId, @jakarta.persistence.Id, or @javax.persistence.Id.");
        }

        for (String id : ID_ORDER) {
            List<Element> idElements = getAllFieldsOrMethodsAnnotatedBy(type, id);
            if (idElements.size() > 1) {
                var idNames = idElements.stream()
                        .map(this::prettyPrintElement)
                        .collect(Collectors.joining(", "));

                processingEnv.getMessager().printMessage(
                        Diagnostic.Kind.ERROR, "[Sprout] Multiple ID fields or methods found in "
                                + type.getQualifiedName() + ": " + idNames + ". "
                );
                throw new IllegalStateException("Multiple ID fields or methods found in " + type.getQualifiedName());
            } else if (idElements.size() == 1) {
                return idElements.getFirst();
            }
        }
        processingEnv.getMessager().printMessage(
                Diagnostic.Kind.ERROR,
                "[Sprout] No ID field or method found in " + type.getQualifiedName() + ". " + "Please annotate " +
                        "one field or method with @SproutId, @jakarta.persistence.Id, or @javax.persistence.Id."
        );
        throw new IllegalStateException("No ID field or method found in " + type.getQualifiedName() + ". " +
                "Please annotate one field or method with @SproutId, @jakarta.persistence.Id, or @javax.persistence.Id."
        );
    }

    private TypeMirror getIdTypeFromElement(Element idElement) {
        var typeMirror = idElement.getKind() == ElementKind.METHOD ?
                ((ExecutableElement) idElement).getReturnType() : idElement.asType();
        return canonicalBoxedErasure(typeMirror);
    }

    private String getIdNameFromElement(Element element) {
        String name = element.getSimpleName().toString();
        return switch (element.getKind()) {
            case FIELD -> name;
            case METHOD -> {
                if (name.startsWith("get") && name.length() > 3) {
                    name = name.substring(3);
                } else if (name.startsWith("is") && name.length() > 2) {
                    name = name.substring(2);
                }
                yield Character.toLowerCase(name.charAt(0)) + name.substring(1);
            }
            default -> name;
        };
    }

    private boolean hasEmbeddedId(TypeElement type) {
        return !getAllAnnotatedBy(type, JAKARTA_EMBEDDED_ID).isEmpty() ||
                !getAllAnnotatedBy(type, JAVAX_EMBEDDED_ID).isEmpty();
    }

    private List<Element> getAllFieldsOrMethodsAnnotatedBy(TypeElement type, String annotationName) {
        return getAllAnnotatedBy(type, annotationName).stream()
                .filter(this::isNonStaticFieldOrMethod)
                .toList();
    }

    private List<Element> getAllAnnotatedBy(TypeElement type, String annotationName) {
        return processingEnv.getElementUtils().getAllMembers(type).stream()
                .filter(Objects::nonNull)
                .map(Element.class::cast)
                .filter(element -> element.getAnnotationMirrors()
                        .stream()
                        .anyMatch(annotationMirror ->
                                annotationMirror.getAnnotationType().toString().equals(annotationName)
                        )
                )
                .toList();
    }

    private String resolveJpaEntityName(TypeElement type) {
        var annotationMirror = findAnnotation(type, JAKARTA_ENTITY);
        if (annotationMirror == null) {
            annotationMirror = findAnnotation(type, JAVAX_ENTITY);
        }
        if (annotationMirror == null) {
            return type.getSimpleName().toString();
        }
        return getAnnotationStringValue(annotationMirror, "name")
                .filter(s -> !s.isBlank())
                .orElse(type.getSimpleName().toString());
    }

    private AnnotationMirror findAnnotation(Element element, String annotationName) {
        return element.getAnnotationMirrors().stream()
                .filter(annotationMirror -> annotationMirror.getAnnotationType().toString().equals(annotationName))
                .findFirst()
                .orElse(null);
    }

    private Optional<String> getAnnotationStringValue(AnnotationMirror annotationMirror, String attributeName) {
        return annotationMirror.getElementValues().entrySet().stream()
                .filter(entry -> entry.getKey().getSimpleName().contentEquals(attributeName))
                .map(Map.Entry::getValue)
                .map(AnnotationValue::getValue)
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .findFirst();
    }

    private boolean isNonStaticFieldOrMethod(Element element) {
        if (element.getKind().isField()) {
            return !element.getModifiers().contains(Modifier.STATIC);
        } else if (element.getKind() == ElementKind.METHOD) {
            ExecutableElement method = (ExecutableElement) element;
            return !method.getModifiers().contains(Modifier.STATIC) &&
                    method.getParameters().isEmpty() &&
                    method.getReturnType().getKind() != TypeKind.VOID;
        }
        return false;
    }

    private TypeMirror canonicalBoxedErasure(TypeMirror type) {
        var types = processingEnv.getTypeUtils();
        if (type.getKind().isPrimitive()) {
            return types.boxedClass((PrimitiveType) type).asType();
        } else {
            return types.erasure(type);
        }
    }

    private String prettyPrintElement(Element element) {
        return switch (element.getKind()) {
            case FIELD -> "Field: '" + element.getSimpleName() + "'";
            case METHOD -> "Method: '" + element.getSimpleName() + "()'";
            default -> element.getSimpleName().toString();
        };
    }

    private JavaFile createJavaFile(String pkg, TypeSpec.Builder type) {
        addGeneratedAnnotation(type);
        return JavaFile.builder(pkg, type.build())
                .skipJavaLangImports(true)
                .indent("    ")
                .build();
    }

    private void addGeneratedAnnotation(TypeSpec.Builder typeBuilder) {
        typeBuilder.addAnnotation(
                AnnotationSpec.builder(ClassName.get("javax.annotation.processing", "Generated"))
                        .addMember("value", "$S", "SproutProcessor")
                        .build()
        );
    }

    private void writeFiles(JavaFile... files) {
        try {
            for (JavaFile javaFile : files) {
                javaFile.writeTo(processingEnv.getFiler());
            }
        } catch (IOException ex) {
            processingEnv.getMessager().printMessage(
                    Diagnostic.Kind.ERROR, "[Sprout] Failed to write generated sources: " + ex.getMessage()
            );
        }
    }

    private boolean hasClass(String typeName) {
        return processingEnv.getElementUtils().getTypeElement(typeName) != null;
    }
}
