// Copyright 2015 Intendia, SL.
package com.intendia.qualifier.processor;

import static com.intendia.qualifier.PropertyQualifier.PROPERTY_GETTER;
import static com.intendia.qualifier.PropertyQualifier.PROPERTY_PATH;
import static com.intendia.qualifier.PropertyQualifier.PROPERTY_SETTER;
import static com.squareup.javapoet.MethodSpec.methodBuilder;
import static javax.lang.model.element.Modifier.PUBLIC;

import com.intendia.qualifier.PropertyQualifier;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import java.util.function.BiConsumer;
import java.util.function.Function;
import javax.lang.model.element.ExecutableElement;

public class PropertyQualifierProcessorProvider extends QualifierProcessorServiceProvider {
    private static final ClassName LANG_STRING = ClassName.get(String.class);

    @Override public void processProperty(TypeSpec.Builder writer, Metamodel descriptor) {
        if (!descriptor.isProperty()) return;

        // Bean ex. ref: person, name: self, type: Person
        ClassName beanType = ClassName.get(descriptor.beanElement());
        // Property ex. ref: person.address, name: address, type: Address
        TypeName propertyType = TypeName.get(descriptor.propertyType());

        // extends PropertyQualifier<BeanT,PropertyT>
        writer.addSuperinterface(ParameterizedTypeName.get(
                ClassName.get(PropertyQualifier.class), beanType, propertyType));

        // Property path
        writer.addMethod(methodBuilder("getPath")
                .addModifiers(PUBLIC)
                .returns(LANG_STRING)
                .addStatement("return getName()")
                .build());
        descriptor.metadata().put(PROPERTY_PATH).valueBlock("$L()", "getPath");

        // Property getter
        final ExecutableElement getter = descriptor.getterElement();
        if (getter != null) {
            // getGetter()
            MethodSpec.Builder getMethod = methodBuilder("getGetter").addModifiers(PUBLIC)
                    .returns(ParameterizedTypeName.get(ClassName.get(Function.class), beanType, propertyType));
            if (getter.getParameters().isEmpty()) {
                getMethod.addStatement("return $T::$N", beanType, getter.getSimpleName());
            } else {
                final TypeName categoryName = ClassName.get(getter.getEnclosingElement().asType());
                getMethod.addStatement("return $T::$N", categoryName, getter.getSimpleName());

            }
            writer.addMethod(getMethod.build());
            descriptor.metadata().literal(PROPERTY_GETTER, "getGetter()");
        }

        // Property setter
        final ExecutableElement setter = descriptor.setterElement();
        if (setter != null) {
            // getSetter()
            MethodSpec.Builder getMethod = methodBuilder("getSetter").addModifiers(PUBLIC)
                    .returns(ParameterizedTypeName.get(ClassName.get(BiConsumer.class), beanType, propertyType));
            if (setter.getParameters().size() == 1) {
                getMethod.addStatement("return $T::$N", beanType, setter.getSimpleName());
            } else {
                final TypeName categoryName = ClassName.get(setter.getEnclosingElement().asType());
                getMethod.addStatement("return $T::$N", categoryName, setter.getSimpleName());

            }
            writer.addMethod(getMethod.build());
            descriptor.metadata().literal(PROPERTY_SETTER, "getSetter()");
        }
    }
}
