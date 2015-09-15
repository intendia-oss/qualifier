// Copyright 2015 Intendia, SL.
package com.intendia.qualifier;

import java.util.function.Function;
import javax.annotation.Nullable;

public interface Metadata {
    @Nullable Object get(String key);

    @SuppressWarnings("unchecked") default @Nullable <T> T get(Extension<T> key) { return (T) get(key.getKey()); }

    default Metadata prototype(Metadata prototype) {
        return key -> { Object t = this.get(key); if (t != null) return t; return prototype.get(key); };
    }

    Metadata EMPTY = readOnly(key -> null);

    static Metadata readOnly(Function<String, Object> metadata) { return metadata::apply; }
}
