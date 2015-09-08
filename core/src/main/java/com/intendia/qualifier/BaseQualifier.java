// Copyright 2013 Intendia, SL.
package com.intendia.qualifier;

import static com.google.common.collect.Ordering.from;
import static com.google.common.collect.Ordering.natural;
import static com.google.common.collect.Ordering.usingToString;

import com.google.common.collect.Ordering;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import javax.annotation.Nullable;

public abstract class BaseQualifier<T, V> implements Qualifier<T, V>
//        RepresenterQualifier<T, V>,
//        I18nQualifier<T, V>,
//        MeasureQualifier<T, V>
{
    private final static Ordering<Object> objectComparator = usingToString().nullsFirst();
    private final static Ordering<Comparable<?>> comparableComparator = natural().nullsFirst();
    private final static Ordering<String> stringComparator = from(String.CASE_INSENSITIVE_ORDER).nullsFirst();

    private final Comparator<T> runtimeComparator = new Comparator<T>() {
        // private Renderer<V> renderer;

        @Override public int compare(T o1, T o2) {
            // TODO use compilation time comparator
            V left = o1 == null ? null : get(o1), right = o2 == null ? null : get(o2);
            if (left == null || right == null) {
                return objectComparator.compare(left, right);
//            } else if (!(left instanceof Comparable<?>)) {
//                if (renderer == null) renderer = getRenderer();
//                return stringComparator.compare(renderer.render(left), renderer.render(right));
            } else if (left instanceof String) {
                return stringComparator.compare((String) left, (String) right);
            } else {
                return comparableComparator.compare((Comparable<?>) left, (Comparable<?>) right);
            }
        }
    };

    @Override public Class<?>[] getGenerics() { return new Class[0]; }

    @Override public String getPath() { return getName(); }

    @Override public @Nullable V get(T object) {
        throw new UnsupportedOperationException("Property " + getName() + " is not readable");
    }

    @Override public Boolean isReadable() { return Boolean.FALSE; }

    @Override public void set(T object, V value) {
        throw new UnsupportedOperationException("Property " + getName() + " is not settable");
    }

    @Override public Boolean isWritable() { return Boolean.FALSE; }

    @Override public Comparator<? super T> getComparator() { return runtimeComparator; }

    @Override public <U> Qualifier<T, U> as(final Qualifier<? super V, U> property) {
        return new PathQualifier<>(this, property);
    }

    @Override public Map<String, Object> getContext() { return Collections.emptyMap(); }

    @Override public String toString() { return getPath() + "[" + getType() + "]"; }
}
