package com.intendia.qualifier;

import com.google.gwt.cell.client.Cell;
import com.google.gwt.text.shared.Renderer;
import com.google.gwt.text.shared.SafeHtmlRenderer;
import java.util.Comparator;
import java.util.Map;
import javax.annotation.Nullable;
import javax.measure.quantity.Quantity;
import javax.measure.unit.Unit;

/**
 * Gets a {@code T -> U -> V} and return {@code T -> V}.
 * 
 * @param <T> the type of the bean (a.k.a. qualified bean)
 * @param <V> the type of the destination value
 * @param <U> the type of the origin value
 */
class PathQualifier<T, V, U> implements PropertyQualifier<T, V> {
    private final Qualifier<T, U> parent;
    private final Qualifier<U, V> child;

    public PathQualifier(Qualifier<T, U> parent, Qualifier<U, V> child) {
        this.parent = parent;
        this.child = child;
    }

    @Override
    public String getName() {
        return child.getName();
    }

    @Override
    public String getPath() {
        return parent.getPath() + "." + child.getPath();
    }

    @Override
    public Class<V> getType() {
        return child.getType();
    }

    @Override
    @Nullable
    public V get(T instance) {
        U value = parent.get(instance);
        if (value == null) return null;
        return child.get(value);
    }

    @Override
    public Boolean isReadable() {
        return parent.isReadable() && child.isReadable();
    }

    @Override
    public void set(T object, V value) {
        child.set(parent.get(object), value);
    }

    @Override
    public Boolean isWritable() {
        return parent.isWritable() && child.isWritable();
    }

    @Override
    public Comparator<? super T> getComparator() {
        return new Comparator<T>() {
            @Override
            public int compare(T o1, T o2) {
                return child.getComparator().compare(
                        o1 == null ? null : parent.get(o1),
                        o2 == null ? null : parent.get(o2));
            }
        };
    }

    @Override
    public Renderer<V> getRenderer() {
        return child.getRenderer();
    }

    @Override
    public SafeHtmlRenderer<V> getSafeHtmlRenderer() {
        return child.getSafeHtmlRenderer();
    }

    @Override
    public Cell<V> getCell() {
        return child.getCell();
    }

    @Override
    public String summary() {
        return child.summary();
    }

    @Override
    public String abbreviation() {
        return child.abbreviation();
    }

    @Override
    public String description() {
        return child.description();
    }

    @Override
    public Unit<? extends Quantity> unit() {
        return child.unit();
    }

    @Override
    public Class<? extends Quantity> quantity() {
        return child.quantity();
    }

    @Override
    public <ValueV> Qualifier<T, ValueV> as(Qualifier<V, ValueV> property) {
        return new PathQualifier<>(this, property);
    }

    @Override
    public Map<String, Object> getContext() {
        return child.getContext();
    }

    @Nullable
    @Override
    public V apply(@Nullable T input) {
        return get(input);
    }
}
