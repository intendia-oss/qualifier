// Copyright 2015 Intendia, SL.
package com.intendia.qualifier.processor;

import static com.intendia.qualifier.PropertyQualifier.PROPERTY_GETTER;
import static com.intendia.qualifier.PropertyQualifier.PROPERTY_PATH;
import static com.intendia.qualifier.PropertyQualifier.PROPERTY_SETTER;
import static javax.lang.model.element.Modifier.FINAL;

import com.intendia.qualifier.PropertyQualifier;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import java.util.function.BiConsumer;
import java.util.function.Function;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;

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
        descriptor.metadata().literal(PROPERTY_PATH, "getName()");

        final ExecutableElement getter = descriptor.getterElement();
        final ExecutableElement setter = descriptor.setterElement();
        final VariableElement field = descriptor.fieldElement();
        final boolean fieldReadable = field != null;
        final boolean fieldWritable = field != null && !field.getModifiers().contains(FINAL);

        // Property getter
        if (getter != null || fieldReadable) {
            CodeBlock.Builder g = CodeBlock.builder();
            g.add("($T) ", ParameterizedTypeName.get(ClassName.get(Function.class), beanType, propertyType));
            if (getter == null) g.add("t -> t.$N", field.getSimpleName());
            else if (getter.getParameters().isEmpty()) g.add("$T::$N", beanType, getter.getSimpleName());
            else g.add("$T::$N", ClassName.get(getter.getEnclosingElement().asType()), getter.getSimpleName());
            descriptor.metadata().literal(PROPERTY_GETTER, g.build());
        }

        // Property setter
        if (setter != null || fieldWritable) {
            CodeBlock.Builder s = CodeBlock.builder();
            s.add("($T) ", ParameterizedTypeName.get(ClassName.get(BiConsumer.class), beanType, propertyType));
            if (setter == null) s.add("(t, v) -> t.$N = v", field.getSimpleName());
            else if (setter.getParameters().size() == 1) s.add("$T::$N", beanType, setter.getSimpleName());
            else s.add("$T::$N", ClassName.get(setter.getEnclosingElement().asType()), setter.getSimpleName());
            descriptor.metadata().literal(PROPERTY_SETTER, s.build());
        }
    }
}
