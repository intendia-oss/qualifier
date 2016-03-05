// Copyright 2013 Intendia, SL.
package com.intendia.qualifier;

import java.util.Comparator;
import java.util.function.BiConsumer;
import java.util.function.Function;
import javax.annotation.Nullable;

/**
 * @param <T> the type of the bean (a.k.a. qualified bean)
 * @param <V> the type of the value (a.k.a qualified property)
 */
@FunctionalInterface
public interface PropertyQualifier<T, V> extends Qualifier<V> {
    Extension<String> PROPERTY_PATH = Extension.key("property.path");
    Extension<Function<?, ?>> PROPERTY_GETTER = Extension.key("property.getter");
    Extension<Boolean> PROPERTY_READABLE = Extension.key("property.readable");
    Extension<BiConsumer<?, ?>> PROPERTY_SETTER = Extension.key("property.setter");
    Extension<Boolean> PROPERTY_WRITABLE = Extension.key("property.writable");
    Extension<Comparator<?>> PROPERTY_COMPARATOR = Extension.key("property.comparator");

    default String getPath() { return data(PROPERTY_PATH, ""); }

    //XXX experimental: utility to extract path without name
    default String getPath(String concat) {
        final String path = getPath();
        if (path.isEmpty()) return concat;
        else return getPath().substring(0, path.length() - getName().length()) + concat;
    }

    // TODO add Nullable/Nonnull annotations using processor extensions
    @SuppressWarnings("NullableProblems") default V get(T object) {
        return opt(PROPERTY_GETTER.<Function<T, V>>as())
                .orElseThrow(() -> new UnsupportedOperationException("Property " + getName() + " is not readable"))
                .apply(object);
    }

    default Boolean isReadable() { return data(PROPERTY_READABLE, Boolean.FALSE); }

    @SuppressWarnings("NullableProblems") default void set(T object, V value) {
        opt(PROPERTY_SETTER.<BiConsumer<T, V>>as())
                .orElseThrow(() -> new UnsupportedOperationException("Property " + getName() + " is not writable"))
                .accept(object, value);
    }

    default Boolean isWritable() { return data(PROPERTY_WRITABLE, Boolean.FALSE); }

    default Comparator<T> getPropertyComparator() {
        return data(PROPERTY_COMPARATOR.<Comparator<T>>as(), ComparableQualifier.of(this).orderingOnResultOf(this::get));
    }

    /** Traverse a qualifier returning a new qualifier which has source type this and value type property. */
    default <U> PropertyQualifier<T, U> compose(PropertyQualifier<? super V, U> property) {
        return new CompositionPropertyQualifier<>(this, property);
    }

    default @Nullable PropertyQualifier<T, ?> compose(String name) {
        PropertyQualifier<V, ?> property = getProperty(name);
        return property == null ? null : compose(property);
    }

    // XXX GWT get confused if this.override2() overrides super.override()
    default <E> PropertyQualifier<T, V> override2(Extension<E> extension, E value) {
        return str -> extension.getKey().equals(str) ? value : data(extension);
    }

    static <V> PropertyQualifier<V, V> asProperty(Qualifier<V> q) { return new IdentityPropertyQualifier<>(q); }

    static <V> PropertyQualifier<?, V> of(Qualifier<V> q) {
        return q instanceof PropertyQualifier ? (PropertyQualifier<?, V>) q : q::data;
    }
}

class IdentityPropertyQualifier<X> implements PropertyQualifier<X, X> {
    private final Qualifier<X> f;

    IdentityPropertyQualifier(Qualifier<X> f) { this.f = f; }

    @Nullable @Override public Object data(String key) { return f.data(key); }

    @Override public @Nullable X get(X object) { return object; }

    @Override public Boolean isReadable() { return true; }

    @Override public Comparator<X> getPropertyComparator() { return ComparableQualifier.of(f).getTypeComparator(); }
}

/**
 * The property qualifiers f : X → Y and g : Y → Z can be composed to yield a qualifier which maps x in X to g(f(x)) in
 * Z. The resulting composite function is denoted g ∘ f : X → Z and has the qualifier type Z.
 */
class CompositionPropertyQualifier<X, Y, Z> implements PropertyQualifier<X, Z> {
    private final PropertyQualifier<X, Y> f;
    private final PropertyQualifier<? super Y, Z> g;

    CompositionPropertyQualifier(PropertyQualifier<X, Y> f, PropertyQualifier<? super Y, Z> g) {
        this.f = f;
        this.g = g;
    }

    @Nullable @Override public Object data(String key) { return g.data(key); }

    @Override public String getName() { return g.getName(); }

    @Override public String getPath() { return f.getPath() + "." + g.getPath(); }

    @Override public Class<Z> getType() { return g.getType(); }

    @Override public Class<?>[] getGenerics() { return g.getGenerics(); }

    @Override @Nullable public Z get(X object) {
        Y value = f.get(object); if (value == null) return null; return g.get(value);
    }

    @Override public Boolean isReadable() { return f.isReadable() && g.isReadable(); }

    @Override public void set(X object, Z value) { g.set(f.get(object), value); }

    @Override public Boolean isWritable() { return f.isWritable() && g.isWritable(); }

    @Override public Comparator<X> getPropertyComparator() {
        return (o1, o2) -> g.getPropertyComparator().compare(
                o1 == null ? null : f.get(o1),
                o2 == null ? null : f.get(o2));
    }
}
