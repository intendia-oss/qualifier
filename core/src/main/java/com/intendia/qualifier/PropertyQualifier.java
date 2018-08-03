package com.intendia.qualifier;

import static java.util.Objects.requireNonNull;

import java.util.Comparator;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import javax.annotation.Nonnull;
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

    /** @throws RuntimeException if not {@link #isReadable()} */
    default Function<T, V> getGetter() { return req(PROPERTY_GETTER.as()); }

    default Boolean isReadable() { return data(PROPERTY_GETTER) != null; }

    /** @throws RuntimeException if not {@link #isWritable()} */
    default BiConsumer<T, V> getSetter() { return req(PROPERTY_SETTER.as()); }

    default Boolean isWritable() { return data(PROPERTY_SETTER) != null; }

    default Comparator<T> getPropertyComparator() {
        //noinspection Convert2Lambda IGP-1732 GWT optimize incompatible
        return data(PROPERTY_COMPARATOR.as(), new Supplier<Comparator<T>>() {
            @Override public Comparator<T> get() {
                return orderingOnResultOf(getGetter());
            }
        });
    }

    /** Traverse a qualifier returning a new qualifier which has source type this and value type property. */
    default <U> PropertyQualifier<T, U> compose(PropertyQualifier<? super V, U> property) {
        return new CompositionPropertyQualifier<>(this, property);
    }

    default @Nullable PropertyQualifier<T, ?> compose(String name) {
        PropertyQualifier<V, ?> property = getProperty(name);
        return property == null ? null : compose(property);
    }

    default PropertyQualifier<T, V> overrideProperty() {
        return PropertyQualifier.unchecked(override());
    }

    default PropertyQualifier<T, V> overrideProperty(Consumer<Mutadata> fn) {
        return PropertyQualifier.unchecked(override(fn));
    }

    @SuppressWarnings("unchecked")
    static <T, V> PropertyQualifier<T, V> unchecked(Metadata q) {
        return q instanceof PropertyQualifier ? (PropertyQualifier<T, V>) q : q::data;
    }

    static <V> PropertyQualifier<?, V> of(Qualifier<V> q) {
        return q instanceof PropertyQualifier ? (PropertyQualifier<?, V>) q : q::data;
    }

    static <V> PropertyQualifier<V, V> asProperty(Qualifier<V> q) { return new IdentityPropertyQualifier<>(q); }
}

class IdentityPropertyQualifier<X> implements PropertyQualifier<X, X> {
    private final Qualifier<X> f;

    IdentityPropertyQualifier(Qualifier<X> f) { this.f = f; }

    @Override public @Nullable Object data(@Nonnull String key) {
        switch (key) {
            case PROPERTY_GETTER_KEY: return getGetter();
            case PROPERTY_COMPARATOR_KEY: return getPropertyComparator();
            default: return f.data(key);
        }
    }

    @Override public Function<X, X> getGetter() { return Function.identity(); }

    @Override public Comparator<X> getPropertyComparator() { return f.getTypeComparator(); }
}

/**
 * The property qualifiers f : X → Y and g : Y → Z can be composed to yield a qualifier which maps x in X to g(f(x)) in
 * Z. The resulting composite function is denoted g ∘ f : X → Z and has the qualifier type Z.
 */
class CompositionPropertyQualifier<X, Y, Z> implements PropertyQualifier<X, Z> {
    private final PropertyQualifier<X, Y> f;
    private final PropertyQualifier<? super Y, Z> g;
    private final Function<X, Z> getter;
    private final BiConsumer<X, Z> setter;

    CompositionPropertyQualifier(PropertyQualifier<X, Y> f, PropertyQualifier<? super Y, Z> g) {
        this.f = f;
        this.g = g;

        if (f.isReadable() && g.isReadable()) {
            Function<X, Y> fGetter = f.getGetter();
            Function<? super Y, Z> gGetter = g.getGetter();
            this.getter = x -> {
                Y y = fGetter.apply(x);
                return y == null ? null : gGetter.apply(y);
            };
        } else {
            this.getter = null;
        }

        if (f.isReadable() && g.isWritable()) {
            BiConsumer<? super Y, Z> gSetter = g.getSetter();
            Function<X, Y> fGetter = f.getGetter();
            this.setter = (x, z) -> {
                Y y = requireNonNull(fGetter.apply(x), "property " + f.getType() + "." + f.getName() + " required "
                        + "to set composed property " + g.getType() + "." + g.getName());
                gSetter.accept(y, z);
            };
        } else {
            this.setter = null;
        }
    }

    @Override public @Nullable Object data(@Nonnull String key) {
        switch (key) {
            case CORE_NAME_KEY: return getName();
            case CORE_TYPE_KEY: return getType();
            case CORE_GENERICS_KEY: return getGenerics();
            case PROPERTY_PATH_KEY: return getPath();
            case PROPERTY_GETTER_KEY: return getter;
            case PROPERTY_SETTER_KEY: return setter;
            case PROPERTY_COMPARATOR_KEY: return getPropertyComparator();
            default: return g.data(key);
        }
    }

    @Override public String getName() { return g.getName(); }

    @Override public Class<Z> getType() { return g.getType(); }

    @Override public String getPath() { return f.getPath() + "." + g.getPath(); }

    @Override public Class<?>[] getGenerics() { return g.getGenerics(); }

    @Override public Comparator<X> getPropertyComparator() {
        Function<X, Y> fGetter = f.getGetter();
        Comparator<? super Y> gPropertyComparator = g.getPropertyComparator();
        return (o1, o2) -> gPropertyComparator.compare(
                o1 == null ? null : fGetter.apply(o1),
                o2 == null ? null : fGetter.apply(o2));
    }
}
