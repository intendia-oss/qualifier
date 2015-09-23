// Copyright 2015 Intendia, SL.
package com.intendia.qualifier;

import java.util.Objects;
import java.util.Optional;
import javax.annotation.Nullable;

@FunctionalInterface
public interface Metadata {
    @Nullable Object data(String key);

    @SuppressWarnings("unchecked")
    default @Nullable <T> T data(Extension<T> key) { return (T) data(key.getKey()); }

    default <T> T data(Extension<T> key, T or) { T v = data(key); return v != null ? v : Objects.requireNonNull(or); }

    default <T> Optional<T> opt(Extension<T> key) { return Optional.ofNullable(data(key)); }
}
