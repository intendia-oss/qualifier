// Copyright 2015 Intendia, SL.
package com.intendia.qualifier;

import static java.util.Objects.requireNonNull;

import java.util.Comparator;
import java.util.Objects;
import java.util.function.Function;
import javax.annotation.Nullable;

public interface ComparableQualifier<T> extends Qualifier<T> {
    Extension<Comparator<?>> COMPARABLE_COMPARATOR = Extension.key("comparable.comparator");
    Comparator<?> TO_STRING_COMPARATOR = new NullsFirstComparator<>(comparing(Objects::toString));

    // TODO choose comparator in processor
    default Comparator<T> getTypeComparator() {
        //noinspection unchecked
        return data(COMPARABLE_COMPARATOR.as(), (Comparator<T>) TO_STRING_COMPARATOR);
    }

    default <F> Comparator<F> orderingOnResultOf(Function<F, ? extends T> function) {
        return new ByFunctionComparator<>(function, getTypeComparator());
    }

    static <T> ComparableQualifier<T> of(Qualifier<T> q) {
        return q instanceof ComparableQualifier ? (ComparableQualifier<T>) q : q::data;
    }

    static <T, U extends Comparable<? super U>> Comparator<T> comparing(Function<? super T, ? extends U> keyExtractor) {
        Objects.requireNonNull(keyExtractor);
        return (c1, c2) -> keyExtractor.apply(c1).compareTo(keyExtractor.apply(c2));
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

    static final int LEFT_IS_GREATER = 1;
    static final int RIGHT_IS_GREATER = -1;
}

final class ByFunctionComparator<F, T> implements Comparator<F> {
    final Function<F, ? extends T> function;
    final Comparator<T> ordering;

    ByFunctionComparator(Function<F, ? extends T> function, Comparator<T> ordering) {
        this.function = requireNonNull(function);
        this.ordering = requireNonNull(ordering);
    }

    @Override public int compare(F left, F right) {
        return ordering.compare(function.apply(left), function.apply(right));
    }

    @Override public String toString() {
        return ordering + ".onResultOf(" + function + ")";
    }
}
