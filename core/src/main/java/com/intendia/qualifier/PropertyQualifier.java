// Copyright 2013 Intendia, SL.
package com.intendia.qualifier;

import java.util.Comparator;

/**
 * @param <T> the type of the bean (a.k.a. qualified bean)
 * @param <V> the type of the value (a.k.a qualified property)
 */
public interface PropertyQualifier<T, V> extends Qualifier<V> {
    Extension<String> PROPERTY_PATH = Extension.key("core.path");

    default String getPath() { return data(PROPERTY_PATH); }

    // TODO add Nullable/Nonnull annotations using processor extensions
    @SuppressWarnings("NullableProblems") default V get(T object) {
        throw new UnsupportedOperationException("Property " + getName() + " is not readable");
    }

    default Boolean isReadable() { return Boolean.FALSE; }

    @SuppressWarnings("NullableProblems") default void set(T object, V value) {
        throw new UnsupportedOperationException("Property " + getName() + " is not settable");
    }

    default Boolean isWritable() { return Boolean.FALSE; }

    // TODO rename as getBeanComparator (or similar, something indicating that this applies to parent)
    default Comparator<? super T> getComparator() {
        return ComparableQualifier.of(this).getOrdering().onResultOf(this::get);
    }

    /** Traverse a qualifier returning a new qualifier which has source type this and value type property. */
    default <U> PropertyQualifier<T, U> as(PropertyQualifier<? super V, U> property) {
        return new PathQualifier<>(this, property);
    }
}
