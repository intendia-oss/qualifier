// Copyright 2015 Intendia, SL.
package com.intendia.qualifier.example;

import com.intendia.qualifier.Extension;
import com.intendia.qualifier.Qualifier;

public interface ExampleManualExtension<V> extends Qualifier<V> {
    Extension<String> STRING = Extension.key("simple.string");
    Extension<Integer> INTEGER = Extension.key("simple.integer");
    Extension<Class<?>> TYPE = Extension.key("simple.type");

    default String getExampleString() { return data(STRING, "default"); }

    default Integer getExampleInteger() { return data(INTEGER, 0); }

    default Class<?> getExampleType() { return data(TYPE, Object.class); }

    static <V> ExampleManualExtension<V> of(Qualifier<V> q) {
        return q instanceof ExampleManualExtension ? (ExampleManualExtension<V>) q : q::data;
    }
}
