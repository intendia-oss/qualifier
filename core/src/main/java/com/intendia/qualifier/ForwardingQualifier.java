package com.intendia.qualifier;

import com.google.common.collect.ForwardingObject;
import java.util.Comparator;
import javax.annotation.Nullable;

/**
 * An qualifier which forwards all its method calls to another qualifier. Subclasses should override one or more methods
 * to modify the behavior of the backing qualifier as desired per the <a href="http://en.wikipedia.org/wiki/Decorator_pattern">decorator
 * pattern</a>.
 */
public abstract class ForwardingQualifier<T, V> extends ForwardingObject implements PropertyQualifier<T, V> {
    protected ForwardingQualifier() {}

    @Override protected abstract PropertyQualifier<T, V> delegate();

    @Override public Class<?>[] getGenerics() { return delegate().getGenerics(); }

    @Override public String getPath() { return delegate().getPath(); }

    @Override public Boolean isReadable() { return delegate().isReadable(); }

    @Override public @Nullable V get(T object) { return delegate().get(object); }

    @Override public Boolean isWritable() { return delegate().isWritable(); }

    @Override public void set(T object, V value) { delegate().set(object, value); }

    @Override public Comparator<? super T> getComparator() { return delegate().getComparator(); }

    @Override public String getName() { return delegate().getName(); }

    @Override public Class<V> getType() { return delegate().getType(); }
}
