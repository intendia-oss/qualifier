// Copyright 2014 Intendia, SL.
package com.intendia.qualifier.processor;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.intendia.qualifier.processor.ReflectionHelper.QualifyExtensionData;

import com.google.common.base.Preconditions;
import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;
import javax.lang.model.element.TypeElement;

public class QualifierContext {
    private final Map<String, QualifyExtensionData> data = new TreeMap<>();
    private final TypeElement classRepresenter;
    private final ReflectionHelper helper;

    public QualifierContext(TypeElement classRepresenter, ReflectionHelper helper) {
        this.classRepresenter = classRepresenter;
        this.helper = helper;
    }

    public <T> T getOrThrow(Class<T> type, String key) {
        return getOrDefault(type, key, null);
    }

    public <T> T getOrDefault(Class<T> type, String key, T defaultValue) {
        final QualifyExtensionData first = data.get(key);
        return checkNotNull(first != null ? doCast(first, type, key) : defaultValue, "%s not found", key);
    }

    public <T> void doIfExists(Class<T> type, String key, Apply<T> apply) throws IOException {
        final QualifyExtensionData data = this.data.get(key);
        if (data != null) apply.apply(doCast(data, type, key));
    }

    private <T> T doCast(QualifyExtensionData data, Class<T> type, String key) {
        Preconditions.checkArgument(data.getType().toString().equals(type.getName()),
                "value type mismatch (element: %s, key: %s, value: %s,  expected type: %s, actual type: %s",
                classRepresenter, key, data.getValue(), type.getName(), data.getType());
        return type.cast(data.getValue());
    }

    public void putIfNotNull(String key, Object value) {
        // TODO notify the key has been overridden
        // Preconditions.checkArgument(!data.containsKey(key), "duplicate key " + key);
        if (value == null || isEmpty(value)) return;

        doPut(key, value);
    }

    /** Recommended convention to add unique values for a type (ex. {@code putIfNotNull(RoundingMode.class, UP)})*/
    public <T> void putIfNotNull(Class<T> type, T value) {
        putIfNotNull(type.getName(), value);
    }

    public void put(String key, Object value) {
        doPut(key, value);
    }

    private void doPut(String key, Object value) {
        Preconditions.checkNotNull(key, "requires non null keys");
        Preconditions.checkNotNull(value, "requires non null values");
        data.put(key, value instanceof QualifyExtensionData
                ? (QualifyExtensionData) value
                : QualifyExtensionData.of(key, helper.getTypeMirror(value.getClass()), value));
    }

    private boolean isEmpty(Object value) {
        return value instanceof String && ((String) value).trim().isEmpty();
    }

    public boolean contains(String key) {
        return data.containsKey(key);
    }

    public boolean isEmpty() {
        return data.isEmpty();
    }

    public Iterable<QualifyExtensionData> getExtensions() {
        return data.values();
    }

    public static interface Apply<T> {
        void apply(T input) throws IOException;
    }

}
