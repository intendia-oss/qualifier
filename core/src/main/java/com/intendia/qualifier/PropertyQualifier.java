// Copyright 2014 Intendia, SL.
package com.intendia.qualifier;

import static java.util.Objects.requireNonNull;

import javax.annotation.Nullable;

/**
 * @param <BeanT> the type of the qualified bean
 * @param <T> the type of the qualified property
 */
public class PropertyQualifier<BeanT, T> extends Qualifier<T> {
    public static <B, P> PropertyQualifier<B, P> of(QualifierContext qualifierContext) {
        return new PropertyQualifier<>(qualifierContext);
    }

    private PropertyQualifier(QualifierContext qualifierContext) {
        super(qualifierContext);
    }

    public static final String GETTER = "property.getter";

    public PropertyGetter<BeanT, T> getGetter() {
        final PropertyGetter<BeanT, T> getter = getContext().getQualifier(GETTER);
        return requireNonNull(getter, "Non readable qualifier " + this);
    }

    public Boolean isReadable() {
        return getContext().getQualifier(GETTER) != null;
    }

    public @Nullable T get(BeanT instance) {
        return getGetter().get(instance);
    }

    public static final String SETTER = "property.setter";

    public PropertySetter<BeanT, T> getSetter() {
        final PropertySetter<BeanT, T> setter = getContext().getQualifier(SETTER);
        return requireNonNull(setter, "Non writable qualifier " + this);
    }

    public Boolean isWritable() {
        return getContext().getQualifier(SETTER) != null;
    }

    public void set(BeanT bean, @Nullable T property) {
        getSetter().set(bean, property);
    }

    public static final String CHAIN = "property.chain";

    public QualifierChain<BeanT, T> getChain() {
        return null;
    }

    public Qualifier<BeanT> flat() {
        return null;
    }

    public <BeanV, Q extends PropertyQualifier<BeanV, BeanT>> PropertyQualifier<BeanV, T> of(Q bean) {
        return new PropertyQualifier<>(new StaticQualifierContext(new QualifierResolver() {
            @Override
            public Object resolve(String extensionKey) {
                throw new UnsupportedOperationException("Not implemented");
            }
        }));
    }

    public static interface PropertyGetter<BeanT, PropertyT> {
        public @Nullable PropertyT get(BeanT instance);
    }

    public static interface PropertySetter<BeanT, PropertyT> {
        public void set(BeanT instance, @Nullable PropertyT value);
    }

    public static class QualifierChain<BeanT, PropertyT> {
        private final PropertyGetter<BeanT, PropertyT> propertyGetter;
        private final Qualifier<BeanT> beanQualifier;
        private final Qualifier<PropertyT> propertyQualifier;

        public QualifierChain(PropertyQualifier<BeanT, PropertyT> propertyQualifier) {
            this(propertyQualifier.getGetter(), propertyQualifier.get)
        }

        public QualifierChain(PropertyGetter<BeanT, PropertyT> propertyGetter, Qualifier<BeanT> beanQualifier, Qualifier<PropertyT> propertyQualifier) {
            this.propertyGetter = propertyGetter;
            this.beanQualifier = beanQualifier;
            this.propertyQualifier = propertyQualifier;
        }
    }
}
