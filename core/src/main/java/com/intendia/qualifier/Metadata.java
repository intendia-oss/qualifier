// Copyright 2015 Intendia, SL.
package com.intendia.qualifier;

import java.util.Map;
import java.util.function.Function;
import javax.annotation.Nullable;

public interface Metadata {
    @Nullable Object get(String key);

    Metadata EMPTY = readOnly(key -> null);

    static Metadata readOnly(Map<String, Object> metadata) { return metadata::get; }

    static Metadata readOnly(Function<String, Object> metadata) { return metadata::apply; }
}
