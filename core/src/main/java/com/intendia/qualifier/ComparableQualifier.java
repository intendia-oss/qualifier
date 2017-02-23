// Copyright 2015 Intendia, SL.
package com.intendia.qualifier;

import static com.intendia.qualifier.ByFunctionComparator.toStringComparator;
import static com.intendia.qualifier.NaturalOrderComparator.natural;
import static java.util.Objects.requireNonNull;

import java.util.Comparator;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import javax.annotation.Nullable;

public interface ComparableQualifier<T> extends Qualifier<T> {
    String COMPARABLE_COMPARATOR_KEY = "comparable.comparator";
    Extension<Comparator<?>> COMPARABLE_COMPARATOR = Extension.key(COMPARABLE_COMPARATOR_KEY);

    default Comparator<T> getTypeComparator() {
        //noinspection unchecked
        return data(COMPARABLE_COMPARATOR.as(), toStringComparator());
    }

    default <F> Comparator<F> orderingOnResultOf(Function<F, ? extends T> function) {
        return ByFunctionComparator.onResultOf(function, getTypeComparator());
    }

    default ComparableQualifier<T> overrideComparable() { return unchecked(override()); }

    default ComparableQualifier<T> overrideComparable(Consumer<Mutadata> fn) { return unchecked(override(fn)); }

    @SuppressWarnings("unchecked")
    static <T> ComparableQualifier<T> unchecked(Metadata q) {
        return q instanceof ComparableQualifier ? (ComparableQualifier<T>) q : q::data;
    }

    static <T> ComparableQualifier<T> of(Qualifier<T> q) {
        return q instanceof ComparableQualifier ? (ComparableQualifier<T>) q : q::data;
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
