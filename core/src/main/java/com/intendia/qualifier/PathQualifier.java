package com.intendia.qualifier;

import java.util.Comparator;
import javax.annotation.Nullable;

/**
 * Gets a {@code T -> U -> V} and return {@code T -> V}.
 *
 * @param <T> the type of the bean (a.k.a. qualified bean)
 * @param <V> the type of the destination value
 * @param <U> the type of the origin value
 */
public class PathQualifier<T, V, U> implements PropertyQualifier<T, V> {
    private final PropertyQualifier<T, U> parent;
    private final PropertyQualifier<? super U, V> child;

    public PathQualifier(PropertyQualifier<T, U> parent, PropertyQualifier<? super U, V> child) {
        this.parent = parent;
        this.child = child;
    }

    @Nullable @Override public Object data(String key) { return child.data(key); }

    @Override public String getName() { return child.getName(); }

    @Override public String getPath() { return parent.getPath() + "." + child.getPath(); }

    @Override public Class<V> getType() { return child.getType(); }

    @Override public Class<?>[] getGenerics() { return child.getGenerics(); }

    @Override @Nullable public V get(T object) {
        U value = parent.get(object); if (value == null) return null; return child.get(value);
    }

    @Override public Boolean isReadable() { return parent.isReadable() && child.isReadable(); }

    @Override public void set(T object, V value) { child.set(parent.get(object), value); }

    @Override public Boolean isWritable() { return parent.isWritable() && child.isWritable(); }

    @Override public Comparator<? super T> getComparator() {
        return (o1, o2) -> child.getComparator().compare(
                o1 == null ? null : parent.get(o1),
                o2 == null ? null : parent.get(o2));
    }
}
