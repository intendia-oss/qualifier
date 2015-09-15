// Copyright 2015 Intendia, SL.
package com.intendia.qualifier.processor;

import static com.google.common.base.Preconditions.checkArgument;

import com.intendia.qualifier.Extension;
import com.intendia.qualifier.annotation.QualifyExtension;
import com.squareup.javapoet.CodeBlock;
import java.util.Collection;
import java.util.Optional;
import javax.annotation.Nullable;
import javax.lang.model.type.TypeMirror;

public interface QualifierMetadata {
    <T> Optional<Entry<T>> entry(Extension<T> key);

    <T> Optional<Entry<T>> entry(String key);

    default <T> Optional<T> value(Extension<T> key) { return entry(key).flatMap(Entry::value); }

    <T> Entry<T> put(Extension<T> key);

    default <T> Entry<T> put(Extension<T> key, Class<T> type) { return put(key).type(type); }

    default <T> Entry<T> put(Extension<T> key, TypeMirror type) { return put(key).type(type); }

    default <T> Entry<T> use(Extension<T> key, T value) { return this.put(key).value(value); }

    default <T> Entry<T> use(Extension<T> key, T value, T defaultValue) {
        return this.put(key).value(value, defaultValue);
    }

    /** Recommended convention to add unique values for a type (ex. {@code put(RoundingMode.class, UP)}) */
    default <T> Entry<T> use(Class<T> type, T value) { return put(Extension.key(type.getName()), type).value(value); }

    default <T> Entry<T> use(String key, T value) { return use(Extension.<T>key(key), value); }

    default <T> Entry<T> use(String key, T value, T defaultValue) {
        return put(Extension.<T>key(key)).value(value, defaultValue);
    }

    default <T> Entry<T> literal(Extension<T> key, String statement, Object... args) {
        return put(key).valueBlock(CodeBlock.builder().add(statement, args).build());
    }

    default <T> Entry<T> literal(String key, String statement, Object... args) {
        return literal(Extension.key(key), statement, args);
    }

    Entry<?> use(QualifyExtension annotation);

    Collection<Entry<?>> getExtensions();

    interface Entry<T> {
        Extension<T> extension();

        /** The extension type class (e.g. {@code Class<java.lang.String>}). */
        Optional<TypeMirror> type();

        Entry<T> type(TypeMirror type);

        Entry<T> type(Class<T> type);

        /** The processor-time value for this extension. Might not be available. */
        Optional<T> value();

        Entry<T> value(T value);

        Entry<T> value(T value, T defaultValue);

        /**
         * Returns the literal representation for this extension value. E.g. a String literal returns {@code "extension
         * value"}, Integer literal returns {@code 123} and SomeType returns {@code SomeType.valueOf("extension
         * value")}.
         */
        Optional<CodeBlock> valueBlock();

        @SuppressWarnings("unchecked") default <V> Entry<V> as(Extension<V> other) {
            checkArgument(other == extension(), "extension mismatch, expected %s but was %s", extension(), other);
            return (Entry<V>) this;
        }

        Entry<T> valueBlock(@Nullable CodeBlock block);

        default Entry<T> valueBlock(String format, Object... args) {
            return valueBlock(CodeBlock.builder().add(format, args).build());
        }

        QualifierMetadata done();
    }
}
