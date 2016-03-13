// Copyright 2013 Intendia, SL.
package com.intendia.qualifier;

import static java.util.Objects.requireNonNull;

import java.util.Comparator;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;
import javax.annotation.Nullable;

/**
 * @param <T> the type of the bean (a.k.a. qualified bean)
 * @param <V> the type of the value (a.k.a qualified property)
 */
@FunctionalInterface
public interface PropertyQualifier<T, V> extends Qualifier<V> {
    String PROPERTY_PATH_KEY = "property.path";
    String PROPERTY_GETTER_KEY = "property.getter";
    String PROPERTY_SETTER_KEY = "property.setter";
    String PROPERTY_COMPARATOR_KEY = "property.comparator";
    Extension<String> PROPERTY_PATH = Extension.key(PROPERTY_PATH_KEY);
    Extension<Function<?, ?>> PROPERTY_GETTER = Extension.key(PROPERTY_GETTER_KEY);
    Extension<BiConsumer<?, ?>> PROPERTY_SETTER = Extension.key(PROPERTY_SETTER_KEY);
    Extension<Comparator<?>> PROPERTY_COMPARATOR = Extension.key(PROPERTY_COMPARATOR_KEY);

    default String getPath() { return data(PROPERTY_PATH, ""); }

    //XXX experimental: utility to extract path without name
    default String getPath(String concat) {
        final String path = getPath();
        if (path.isEmpty()) return concat;
        else return getPath().substring(0, path.length() - getName().length()) + concat;
    }

    /** @throws RuntimeException if not {@link #isReadable()} */
    default Function<T, V> getGetter() {
        return requireNonNull(data(PROPERTY_GETTER.as()), "property " + getName() + " is not readable");
    }

    default Boolean isReadable() { return data(PROPERTY_GETTER) != null; }

    /** @throws RuntimeException if not {@link #isWritable()} */
    default BiConsumer<T, V> getSetter() {
        return requireNonNull(data(PROPERTY_SETTER.as()), "property " + getName() + " is not writable");
    }

    default Boolean isWritable() { return data(PROPERTY_SETTER) != null; }

    default Comparator<T> getPropertyComparator() {
        return data(PROPERTY_COMPARATOR.<Comparator<T>>as(), (Supplier<Comparator<T>>)
                () -> ComparableQualifier.of(this).orderingOnResultOf(getGetter()));
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

    static <V> PropertyQualifier<V, V> asProperty(Qualifier<V> q) {
        return new IdentityPropertyQualifier<>(q);
    }

    static <V> PropertyQualifier<?, V> of(Qualifier<V> q) {
        return q instanceof PropertyQualifier ? (PropertyQualifier<?, V>) q : q::data;
    }
}

class IdentityPropertyQualifier<X> implements PropertyQualifier<X, X> {
    private final Qualifier<X> f;

    IdentityPropertyQualifier(Qualifier<X> f) {
        this.f = f;
    }

    @Override public @Nullable Object data(String key) {
        return f.data(key);
    }

    @Override public Function<X, X> getGetter() {
        return o -> o;
    }

    @Override public Comparator<X> getPropertyComparator() {
        return ComparableQualifier.of(f).getTypeComparator();
    }
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

    @Override public @Nullable Object data(String key) {
        return g.data(key);
    }

    @Override public String getName() {
        return g.getName();
    }

    @Override public String getPath() {
        return f.getPath() + "." + g.getPath();
    }

    @Override public Class<Z> getType() {
        return g.getType();
    }

    @Override public Class<?>[] getGenerics() {
        return g.getGenerics();
    }

    @Override public Function<X, Z> getGetter() {
        return f.getGetter().andThen(g.getGetter());
    }

    @Override public Boolean isReadable() {
        return f.isReadable() && g.isReadable();
    }

    @Override public BiConsumer<X, Z> getSetter() {
        BiConsumer<? super Y, Z> gSetter = g.getSetter();
        Function<X, Y> fGetter = f.getGetter();
        return (x, z) -> gSetter.accept(fGetter.apply(x), z);
    }

    @Override public Boolean isWritable() {
        return f.isReadable() && g.isWritable();
    }

    @Override public Comparator<X> getPropertyComparator() {
        Function<X, Y> fGetter = f.getGetter();
        Comparator<? super Y> fPropertyComparator = g.getPropertyComparator();
        return (o1, o2) -> fPropertyComparator.compare(
                o1 == null ? null : fGetter.apply(o1),
                o2 == null ? null : fGetter.apply(o2));
    }
}
