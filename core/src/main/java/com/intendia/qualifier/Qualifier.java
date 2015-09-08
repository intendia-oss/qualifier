// Copyright 2013 Intendia, SL.
package com.intendia.qualifier;

import java.util.Comparator;
import java.util.Map;
import javax.annotation.Nullable;

/**
 * @param <T> the type of the bean (a.k.a. qualified bean)
 * @param <V> the type of the value (a.k.a qualified property)
 */
public interface Qualifier<T, V> {
    String CORE_NAME = "core.name";
    String CORE_PATH = "core.path";
    String CORE_TYPE = "core.type";
    String CORE_GENERICS = "core.generics";

    default String getName() { return (String) getContext().get(CORE_NAME); }

    default String getPath() { return (String) getContext().get(CORE_PATH); }

    default Class<V> getType() { return (Class) getContext().get(CORE_TYPE); }

    default Class<?>[] getGenerics() { return (Class<?>[]) getContext().get(CORE_GENERICS); }

    /** Return the properties context of this qualifier. */
    Map<String, Object> getContext();

    // TODO Bean interface
    default @Nullable V get(T object) { throw new UnsupportedOperationException("get"); }

    default Boolean isReadable() { throw new UnsupportedOperationException("get"); }

    default void set(T object, V value) { throw new UnsupportedOperationException("get"); }

    default Boolean isWritable() { throw new UnsupportedOperationException("get"); }

    default Comparator<? super T> getComparator() { throw new UnsupportedOperationException("get"); }

    /** Traverse a qualifier returning a new qualifier which has source type this and value type property. */
    default <ValueU> Qualifier<T, ValueU> as(Qualifier<? super V, ValueU> property) {
        throw new UnsupportedOperationException("get");
    }
}
