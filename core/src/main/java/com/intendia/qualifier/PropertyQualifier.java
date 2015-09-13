// Copyright 2013 Intendia, SL.
package com.intendia.qualifier;

import com.google.common.collect.Ordering;
import java.util.Comparator;
import javax.annotation.Nullable;

/**
 * @param <T> the type of the bean (a.k.a. qualified bean)
 * @param <V> the type of the value (a.k.a qualified property)
 */
public interface PropertyQualifier<T, V> extends Qualifier<V> {

    default @Nullable V get(T object) {
        throw new UnsupportedOperationException("Property " + getName() + " is not readable");
    }

    default Boolean isReadable() { return Boolean.FALSE; }

    default void set(T object, V value) {
        throw new UnsupportedOperationException("Property " + getName() + " is not settable");
    }

    default Boolean isWritable() { return Boolean.FALSE; }

    default Comparator<? super T> getComparator() { return Ordering.from(getComparator1()).onResultOf(this::get); }

    /** Traverse a qualifier returning a new qualifier which has source type this and value type property. */
    default <U> PropertyQualifier<T, U> as(PropertyQualifier<? super V, U> property) {
        return new PathQualifier<>(this, property);
    }

//     Ordering<Object> objectComparator = usingToString().nullsFirst();
//     Ordering<Comparable<?>> comparableComparator = natural().nullsFirst();
//     Ordering<String> stringComparator = from(String.CASE_INSENSITIVE_ORDER).nullsFirst();
//     Comparator runtimeComparator = (o1, o2) -> {
//        // TODO use compilation time comparator
//        Object left = o1 == null ? null : get(o1), right = o2 == null ? null : get(o2);
//        if (left == null || right == null) {
//            return objectComparator.compare(left, right);
////            } else if (!(left instanceof Comparable<?>)) {
////                if (renderer == null) renderer = getRenderer();
////                return stringComparator.compare(renderer.render(left), renderer.render(right));
//        } else if (left instanceof String) {
//            return stringComparator.compare((String) left, (String) right);
//        } else {
//            return comparableComparator.compare((Comparable<?>) left, (Comparable<?>) right);
//        }
//    };

}
