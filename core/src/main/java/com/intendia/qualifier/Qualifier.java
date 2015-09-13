// Copyright 2015 Intendia, SL.
package com.intendia.qualifier;

import static com.google.common.base.MoreObjects.firstNonNull;

import com.google.common.collect.Ordering;
import java.util.Collections;
import java.util.Comparator;
import java.util.Set;
import javax.annotation.Nullable;

@FunctionalInterface
public interface Qualifier<V> {
    String CORE_NAME = "core.name";
    String CORE_PATH = "core.path";
    String CORE_TYPE = "core.type";
    String CORE_GENERICS = "core.generics";

    default String getName() { return (String) getContext().get(CORE_NAME); }

    default String getPath() { return (String) getContext().get(CORE_PATH); }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    default Class<V> getType() { return (Class) getContext().get(CORE_TYPE); }

    default Class<?>[] getGenerics() { return (Class<?>[]) getContext().get(CORE_GENERICS); }

    default Comparator<V> getComparator1() { return Ordering.usingToString().nullsFirst(); }

    /** Return the properties context of this qualifier. */
    Metadata getContext(); // { return Metadata.EMPTY; }

    @SuppressWarnings("unchecked")
    default @Nullable <T> T data(String key) { return (T) getContext().get(key); }

    @SuppressWarnings("unchecked")
    default <T> T data(String key, T or) { return firstNonNull((T) getContext().get(key), or); }

    /** Return the property qualifiers of the bean qualifier. */
    default Set<PropertyQualifier<? super V, ?>> getPropertyQualifiers() { return Collections.emptySet(); }

    /**
     * @deprecated created to easy fix self usages in TableBuilder columns, but! this is not required if Paths and
     * metadata are handled independently (path are the getter/setter methods of the property qualifier). If you don't
     * understand this comment, just understand that if you are using this method is because you are doing something
     * wrong! Another way of seen this situation is; why does you need to explain to some one (ex. TableBuilder) how
     * to obtain a property to itself, ie. how to do {@code x -> x}, obviously something is wrong!
     */
    @Deprecated
    default PropertyQualifier<V, V> asProperty() {
        final Qualifier<V> self = this;
        return new PropertyQualifier<V, V>() {
            @Override public Metadata getContext() { return self.getContext(); }

            @Override public @Nullable V get(V object) { return object; }

            @Override public Boolean isReadable() { return true; }

            @Override public Comparator<? super V> getComparator() { return self.getComparator1(); }
        };
    }

    /** Traverse a qualifier returning a new qualifier which has source type this and value type property. */
//    default <ValueU> SimpleQualifier<V> as(PropertyQualifier<? super V, ValueU> property) {
//        return new PathQualifier<>(this, property);
//    }
}
