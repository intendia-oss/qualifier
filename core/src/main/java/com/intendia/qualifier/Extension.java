package com.intendia.qualifier;

import java.util.Objects;

public interface Extension<T> {
    String getKey();

    default boolean isAnonymous() { return false; }

    @SuppressWarnings("unchecked")
    default <V extends T> Extension<V> as() { return (Extension<V>) this; }

    static <T> Extension<T> anonymous() { return new AnonymousExtension<>(); }

    static <T> Extension<T> key(String key) { return new NamedExtension<>(key); }
}

class NamedExtension<T> implements Extension<T> {
    private final String key;

    NamedExtension(String key) { this.key = key.intern(); }

    @Override public String getKey() { return key; }

    @Override public boolean equals(Object o) {
        return this == o || o instanceof NamedExtension && equals((NamedExtension<?>) o);
    }

    public boolean equals(NamedExtension<?> o) { return Objects.equals(key, o.key); }

    @Override public int hashCode() { return Objects.hash(key); }

    @Override public String toString() { return key + "@" + Integer.toHexString(hashCode()); }
}

class AnonymousExtension<T> implements Extension<T> {
    @Override public String getKey() { throw new UnsupportedOperationException("anonymous extension"); }

    @Override public boolean isAnonymous() { return true; }
}
