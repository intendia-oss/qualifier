package com.intendia.qualifier;

import com.google.common.collect.ForwardingObject;
import com.google.gwt.cell.client.Cell;
import com.google.gwt.text.shared.Renderer;
import com.google.gwt.text.shared.SafeHtmlRenderer;
import java.util.Comparator;
import java.util.Map;
import javax.annotation.Nullable;
import javax.measure.quantity.Quantity;
import javax.measure.unit.Unit;

/**
 * An qualifier which forwards all its method calls to another qualifier. Subclasses should override one or more methods
 * to modify the behavior of the backing qualifier as desired per the <a
 * href="http://en.wikipedia.org/wiki/Decorator_pattern">decorator pattern</a>.
 */
public abstract class ForwardingQualifier<T, V>
        extends ForwardingObject implements Qualifier<T, V> {

    /** Constructor for use by subclasses. */
    protected ForwardingQualifier() {}

    @Override
    protected abstract Qualifier<T, V> delegate();

    @Override
    public String getPath() {
        return delegate().getPath();
    }

    @Override
    public Boolean isReadable() {
        return delegate().isReadable();
    }

    @Nullable
    @Override
    public V get(T object) {
        return delegate().get(object);
    }

    @Override
    public Boolean isWritable() {
        return delegate().isWritable();
    }

    @Override
    public void set(T object, V value) {
        delegate().set(object, value);
    }

    @Override
    public String summary() {
        return delegate().summary();
    }

    @Nullable
    @Override
    public String abbreviation() {
        return delegate().abbreviation();
    }

    @Nullable
    @Override
    public String description() {
        return delegate().description();
    }

    @Override
    public Renderer<V> getRenderer() {
        return delegate().getRenderer();
    }

    @Override
    public SafeHtmlRenderer<V> getSafeHtmlRenderer() {
        return delegate().getSafeHtmlRenderer();
    }

    @Override
    public Cell<V> getCell() {
        return delegate().getCell();
    }

    @Override
    public Unit<? extends Quantity> unit() {
        return delegate().unit();
    }

    @Override
    public Class<? extends Quantity> quantity() {
        return delegate().quantity();
    }

    @Override
    public <U> Qualifier<T, U> as(Qualifier<? super V, U> property) {
        return delegate().as(property);
    }

    @Override
    public Comparator<? super T> getComparator() {
        return delegate().getComparator();
    }

    @Override
    public String getName() {
        return delegate().getName();
    }

    @Override
    public Class<V> getType() {
        return delegate().getType();
    }

    @Nullable
    @Override
    public V apply(@Nullable T input) {
        return delegate().apply(input);
    }

    @Override
    public Map<String, Object> getContext() {
        return delegate().getContext();
    }
}
