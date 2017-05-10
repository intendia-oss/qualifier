package com.intendia.qualifier.processor;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import java.util.Collection;
import java.util.List;
import javax.annotation.Nullable;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;

public interface Metamodel {
    String SELF = "self";

    String name();

    DeclaredType beanType();

    TypeElement beanElement();

    TypeMirror propertyType();

    @Nullable TypeElement propertyElement();

    @Nullable ExecutableElement getterElement();

    @Nullable ExecutableElement setterElement();

    @Nullable VariableElement fieldElement();

    Metaqualifier metadata();

    @Nullable PropertyReference extend();

    List<CodeBlock> mixins();

    default Collection<Metaextension<?>> extensions() { return metadata().values(); }

    default boolean isProperty() { return !name().equals(SELF); }

    class PropertyReference {
        public final ClassName bean;
        public final String property;
        public PropertyReference(ClassName bean, String property) {
            this.bean = bean;
            this.property = property;
        }
    }
}
