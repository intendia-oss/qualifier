// Copyright 2015 Intendia, SL.
package com.intendia.qualifier.processor;

import static com.intendia.qualifier.PropertyQualifier.PROPERTY_PATH;
import static javax.lang.model.element.Modifier.PUBLIC;

import com.intendia.qualifier.PropertyQualifier;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
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
        writer.addMethod(MethodSpec.methodBuilder("getPath")
                .addModifiers(PUBLIC)
                .returns(LANG_STRING)
                .addStatement("return getName()")
                .build());
        descriptor.metadata().put(PROPERTY_PATH).valueBlock("$L()", "getPath");

        // Property getter
        final ExecutableElement getter = descriptor.getterElement();
        if (getter != null) {
            // get()
            final MethodSpec.Builder getMethod = MethodSpec.methodBuilder("get")
                    .addModifiers(PUBLIC)
                    .returns(propertyType)
                    .addParameter(beanType, "object");
            if (getter.getParameters().isEmpty()) {
                getMethod.addStatement("return object.$N()", getter.getSimpleName());
            } else {
                final TypeName categoryName = ClassName.get(getter.getEnclosingElement().asType());
                getMethod.addStatement("return $T.$N(object)", categoryName, getter.getSimpleName());

            }
            writer.addMethod(getMethod.build());

            // isReadable()
            writer.addMethod(MethodSpec.methodBuilder("isReadable")
                    .addModifiers(PUBLIC)
                    .returns(TypeName.BOOLEAN.box())
                    .addStatement("return true")
                    .build());
        }

        // Property setter
        final ExecutableElement setter = descriptor.setterElement();
        if (setter != null) {
            // set()
            final MethodSpec.Builder getMethod = MethodSpec.methodBuilder("set")
                    .addModifiers(PUBLIC)
                    .addParameter(beanType, "object")
                    .addParameter(propertyType, "value");
            if (setter.getParameters().size() == 1) {
                getMethod.addStatement("object.$N(value)", setter.getSimpleName());
            } else {
                final TypeName categoryName = ClassName.get(setter.getEnclosingElement().asType());
                getMethod.addStatement("$T.$N(object, value)", categoryName, setter.getSimpleName());

            }
            writer.addMethod(getMethod.build());

            // isWritable()
            writer.addMethod(MethodSpec.methodBuilder("isWritable")
                    .addModifiers(PUBLIC)
                    .returns(TypeName.BOOLEAN.box())
                    .addStatement("return true")
                    .build());
        }
    }
}
