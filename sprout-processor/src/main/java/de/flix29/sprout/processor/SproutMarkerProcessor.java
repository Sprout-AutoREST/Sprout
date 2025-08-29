package de.flix29.sprout.processor;

import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import javax.lang.model.element.Modifier;
import javax.lang.model.type.TypeMirror;

public class SproutMarkerProcessor {

    private SproutMarkerProcessor() {
        // Utility class
    }

    protected static TypeSpec.Builder generateMarker(
            TypeMirror typeMirror, String className, String apiPath, String entityName, String idName
    ) {
        return TypeSpec.classBuilder(className)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addField(FieldSpec
                        .builder(String.class, "PATH", Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                        .initializer("$S", apiPath)
                        .build()
                )
                .addField(FieldSpec
                        .builder(Class.class, "ID_CLASS", Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                        .initializer("$T.class", TypeName.get(typeMirror))
                        .build()
                )
                .addField(FieldSpec
                        .builder(String.class, "ENTITY_NAME", Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                        .initializer("$S", entityName)
                        .build()
                )
                .addField(FieldSpec
                        .builder(String.class, "ID_PROPERTY", Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                        .initializer("$S", idName)
                        .build()
                );
    }
}
