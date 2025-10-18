package de.flix29.sprout.processor;

import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import de.flix29.sprout.annotations.SproutPolicy;

import javax.lang.model.element.Modifier;
import javax.lang.model.type.TypeMirror;

public class SproutMarkerProcessor {

    private SproutMarkerProcessor() {
        // Utility class
    }

    protected static TypeSpec.Builder generateMarker(
            TypeMirror idType,
            String className,
            String apiPath,
            SproutPolicy policy,
            String entityName,
            String idName
    ) {
        var builder = TypeSpec.classBuilder(className)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addField(FieldSpec
                        .builder(String.class, "PATH", Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                        .initializer("$S", apiPath)
                        .build()
                )
                .addField(FieldSpec
                        .builder(Class.class, "ID_CLASS", Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                        .initializer("$T.class", TypeName.get(idType))
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

        if (policy != null) {
            builder.addField(FieldSpec
                    .builder(String.class, "READ_POLICY", Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                    .initializer("$S", policy.read())
                    .build()
            ).addField(FieldSpec
                    .builder(String.class, "CREATE_POLICY", Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                    .initializer("$S", policy.create())
                    .build()
            ).addField(FieldSpec
                    .builder(String.class, "UPDATE_POLICY", Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                    .initializer("$S", policy.update())
                    .build()
            ).addField(FieldSpec
                    .builder(String.class, "DELETE_POLICY", Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                    .initializer("$S", policy.delete())
                    .build()
            );
        }

        return builder;
    }
}
