package com.intendia.qualifier.example;

import com.intendia.qualifier.Extension;
import com.intendia.qualifier.Qualifier;

@FunctionalInterface
public interface ExampleManualQualifier<V> extends Qualifier<V> {
    Extension<String> STRING = Extension.key("simple.string");
    Extension<Integer> INTEGER = Extension.key("simple.integer");
    Extension<Class<?>> TYPE = Extension.key("simple.type");

    default String getExampleString() { return data(STRING, "default"); }

    default Integer getExampleInteger() { return data(INTEGER, 0); }

    default Class<?> getExampleType() { return data(TYPE, Object.class); }

    static <V> ExampleManualQualifier<V> of(Qualifier<V> q) {
        return q instanceof ExampleManualQualifier ? (ExampleManualQualifier<V>) q : q::data;
    }
}
