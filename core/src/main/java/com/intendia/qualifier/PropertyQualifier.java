// Copyright 2013 Intendia, SL.
package com.intendia.qualifier;

import java.util.Comparator;
import javax.annotation.Nullable;

/**
 * @param <T> the type of the bean (a.k.a. qualified bean)
 * @param <V> the type of the value (a.k.a qualified property)
 */
@FunctionalInterface
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
        return ComparableQualifier.of(this).orderingOnResultOf(this::get);
    }

    /** Traverse a qualifier returning a new qualifier which has source type this and value type property. */
    default <U> PropertyQualifier<T, U> as(PropertyQualifier<? super V, U> property) {
        return new CompositionPropertyQualifier<>(this, property);
    }

    static <V> PropertyQualifier<V, V> asProperty(Qualifier<V> q) { return new IdentityPropertyQualifier<>(q); }

}

class IdentityPropertyQualifier<X> implements PropertyQualifier<X, X> {
    private final Qualifier<X> f;

    IdentityPropertyQualifier(Qualifier<X> f) { this.f = f; }

    @Nullable @Override public Object data(String key) { return f.data(key); }

    @Override public @Nullable X get(X object) { return object; }

    @Override public Boolean isReadable() { return true; }

    @Override public Comparator<? super X> getComparator() { return ComparableQualifier.of(f).getOrdering(); }
}

/**
 * The qualifiers f : X → Y and g : Y → Z can be composed to yield a qualifier which maps x in X to g(f(x)) in Z. The
 * resulting composite function is denoted g ∘ f : X → Z
 */
class CompositionPropertyQualifier<X, Y, Z> implements PropertyQualifier<X, Y> {
    private final PropertyQualifier<X, Z> f;
    private final PropertyQualifier<? super Z, Y> g;

    CompositionPropertyQualifier(PropertyQualifier<X, Z> f, PropertyQualifier<? super Z, Y> g) {
        this.f = f;
        this.g = g;
    }

    @Nullable @Override public Object data(String key) { return g.data(key); }

    @Override public String getName() { return g.getName(); }

    @Override public String getPath() { return f.getPath() + "." + g.getPath(); }

    @Override public Class<Y> getType() { return g.getType(); }

    @Override public Class<?>[] getGenerics() { return g.getGenerics(); }

    @Override @Nullable public Y get(X object) {
        Z value = f.get(object); if (value == null) return null; return g.get(value);
    }

    @Override public Boolean isReadable() { return f.isReadable() && g.isReadable(); }

    @Override public void set(X object, Y value) { g.set(f.get(object), value); }

    @Override public Boolean isWritable() { return f.isWritable() && g.isWritable(); }

    @Override public Comparator<? super X> getComparator() {
        return (o1, o2) -> g.getComparator().compare(
                o1 == null ? null : f.get(o1),
                o2 == null ? null : f.get(o2));
    }
}
