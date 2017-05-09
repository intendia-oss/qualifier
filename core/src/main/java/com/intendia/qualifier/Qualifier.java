// Copyright 2015 Intendia, SL.
package com.intendia.qualifier;

import static com.intendia.qualifier.ByFunctionComparator.toStringComparator;
import static com.intendia.qualifier.NaturalOrderComparator.natural;
import static java.util.Collections.emptySet;
import static java.util.Objects.requireNonNull;

import java.util.Collection;
import java.util.Comparator;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import javax.annotation.Nullable;

@FunctionalInterface
@SuppressWarnings("ClassReferencesSubclass")
public interface Qualifier<T> extends Metadata {
    String CORE_NAME_KEY = "core.name";
    String CORE_TYPE_KEY = "core.type";
    String CORE_GENERICS_KEY = "core.generics";
    String CORE_PROPERTIES_KEY = "core.properties";
    String COMPARABLE_COMPARATOR_KEY = "comparable.comparator";
    Extension<String> CORE_NAME = Extension.key(CORE_NAME_KEY);
    Extension<Class<?>> CORE_TYPE = Extension.key(CORE_TYPE_KEY);
    Extension<Class<?>[]> CORE_GENERICS = Extension.key(CORE_GENERICS_KEY);
    Extension<Collection<? extends PropertyQualifier<?, ?>>> CORE_PROPERTIES = Extension.key(CORE_PROPERTIES_KEY);
    Extension<Comparator<?>> COMPARABLE_COMPARATOR = Extension.key(COMPARABLE_COMPARATOR_KEY);
    Class<?>[] NO_GENERICS = new Class[0];

    default String getName() { return data(CORE_NAME); }

    default Class<T> getType() { return data(CORE_TYPE.as()); }

    default Class<?>[] getGenerics() { return data(CORE_GENERICS, NO_GENERICS); }

    /** Return the property qualifiers of the bean qualifier. */
    default Collection<PropertyQualifier<T, ?>> getProperties() { return data(CORE_PROPERTIES.as(), emptySet()); }

    default @Nullable PropertyQualifier<T, ?> getProperty(String name) {
        if (name.isEmpty()) throw new IllegalArgumentException("not empty name required");
        String[] split = name.split("\\.", 2);
        for (PropertyQualifier<T, ?> property : getProperties()) {
            if (split[0].equals(property.getName())) {
                return split.length == 1 ? property : property.compose(split[1]);
            }
        }
        return null;
    }

    default Comparator<T> getTypeComparator() {
        //noinspection unchecked
        return data(COMPARABLE_COMPARATOR.as(), toStringComparator());
    }

    default <F> Comparator<F> orderingOnResultOf(Function<F, ? extends T> function) {
        return ByFunctionComparator.onResultOf(function, getTypeComparator());
    }

    default Qualifier<T> overrideQualifier() { return unchecked(override()); }

    default Qualifier<T> overrideQualifier(Consumer<Mutadata> fn) { return unchecked(override(fn)); }

    @SuppressWarnings("unchecked")
    static <T> Qualifier<T> unchecked(Metadata q) {
        return q instanceof Qualifier ? (Qualifier<T>) q : q::data;
    }

    static <T> Qualifier<T> create(Class<T> type) { return create(type, type.getSimpleName()); }

    static <T> Qualifier<T> create(Class<T> type, String name) {
        return key -> {
            switch (key) {
                case CORE_NAME_KEY: return name;
                case CORE_TYPE_KEY: return type;
                default: return null;
            }
        };
    }

    static <T extends Comparable<T>> Comparator<T> naturalComparator() {
        //noinspection unchecked
        return (Comparator<T>) Defaults.NATURAL_COMPARATOR;
    }

    static <T> Comparator<T> toStringComparator() {
        //noinspection unchecked
        return (Comparator<T>) Defaults.TO_STRING_COMPARATOR;
    }
}

final class NullsFirstComparator<T> implements Comparator<T> {
    final Comparator<? super T> comparator;

    NullsFirstComparator(Comparator<? super T> comparator) { this.comparator = comparator; }

    @Override public int compare(@Nullable T left, @Nullable T right) {
        if (left == right) return 0;
        if (left == null) return RIGHT_IS_GREATER;
        if (right == null) return LEFT_IS_GREATER;
        return comparator.compare(left, right);
    }

    @Override public String toString() {
        return comparator + ".nullsFirst()";
    }

    static final int LEFT_IS_GREATER = 1;
    static final int RIGHT_IS_GREATER = -1;
}

final class Defaults {
    static Comparator<?> TO_STRING_COMPARATOR = new NullsFirstComparator<>(toStringComparator());
    static Comparator<?> NATURAL_COMPARATOR = new NullsFirstComparator<>(natural());
}

final class ByFunctionComparator<F, T> implements java.util.Comparator<F> {

    static <F, T> java.util.Comparator<F> onResultOf(Function<F, ? extends T> fn, Comparator<T> ordering) {
        return new ByFunctionComparator<>(fn, ordering);
    }

    static java.util.Comparator<Object> toStringComparator() {
        //noinspection RedundantTypeArguments
        return ByFunctionComparator.<Object, String>onResultOf(Objects::toString, natural());
    }

    final Function<F, ? extends T> fn;
    final Comparator<T> ordering;

    private ByFunctionComparator(Function<F, ? extends T> fn, Comparator<T> ordering) {
        this.fn = requireNonNull(fn);
        this.ordering = requireNonNull(ordering);
    }

    @Override public int compare(F left, F right) {
        return ordering.compare(fn.apply(left), fn.apply(right));
    }

    @Override public String toString() {
        return ordering + ".onResultOf(" + fn + ")";
    }

}

final class NaturalOrderComparator implements java.util.Comparator<Comparable<Object>> {
    static Comparator<?> INSTANCE = new NaturalOrderComparator();

    public static <T extends Comparable<T>> Comparator<T> natural() {
        //noinspection unchecked
        return (Comparator<T>) INSTANCE;
    }

    @Override public int compare(Comparable<Object> c1, Comparable<Object> c2) {
        return c1.compareTo(c2);
    }

    @Override public String toString() { return "natural()"; }
}
