// Copyright 2015 Intendia, SL.
package com.intendia.qualifier.processor;

import com.intendia.qualifier.annotation.QualifyExtension;
import java.util.Collection;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import javax.lang.model.type.TypeMirror;

public interface QualifierMetadata {
    <T> T getOrThrow(Class<T> type, String key);

    <T> T getOrDefault(Class<T> type, String key, T defaultValue);

    <T> void doIfExists(Class<T> type, String key, Consumer<T> apply);

    void putIfNotNull(String key, @Nullable Object value);

    /** Recommended convention to add unique values for a type (ex. {@code putIfNotNull(RoundingMode.class, UP)}) */
    <T> void putIfNotNull(Class<T> type, @Nullable T value);

    Entry put(String key, Object value);

    Entry put(QualifyExtension annotation);

    Entry put(String key, TypeMirror type, String value);

    Entry putClass(String key, String className);

    Entry putLiteral(String key, String literalValue);

    boolean contains(String key);

    Collection<Entry> getExtensions();

    interface Entry {
        String getKey();

        /** The extension type class (e.g. {@code Class<java.lang.String>}). */
        TypeMirror getType();

        /** The processor-time value for this extension. */
        Object getValue();

        <T> T getValue(Class<T> type);

        /**
         * Returns the literal representation for this extension value. E.g. a String literal returns {@code "extension
         * value"}, Integer literal returns {@code 123} and SomeType returns {@code SomeType.valueOf("extension
         * value")}.
         */
        String toLiteral();
    }
}
