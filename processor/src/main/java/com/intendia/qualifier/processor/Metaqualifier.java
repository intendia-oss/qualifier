// Copyright 2015 Intendia, SL.
package com.intendia.qualifier.processor;

import com.intendia.qualifier.Extension;
import com.intendia.qualifier.annotation.Qualify;
import com.squareup.javapoet.CodeBlock;
import java.util.Collection;
import java.util.Optional;
import javax.lang.model.type.TypeMirror;

public interface Metaqualifier {

    <T> Optional<Metaextension<T>> get(Extension<T> key);

    <T> Metaextension<T> put(Extension<T> key);

    Metaextension<?> use(Qualify.Entry annotation);

    Collection<Metaextension<?>> values();

    void remove(Extension<?> key);

    // Helpers

    default <T> Optional<Metaextension<T>> get(String key) {
        return get(Extension.key(key));
    }

    default <T> Optional<T> value(Extension<T> key) {
        return get(key).flatMap(Metaextension::value);
    }

    default <T> Metaextension<T> put(Extension<T> key, TypeMirror type) {
        return put(key).type(type);
    }

    default <T> Metaextension<T> use(Extension<T> key, T value) {
        return put(key).value(value);
    }

    default <T> Metaextension<T> use(Extension<T> key, T value, T ignoreIfEqual) {
        return put(key).value(value, ignoreIfEqual);
    }

    default <T> Metaextension<T> use(String key, T value) {
        return use(Extension.key(key), value);
    }

    default <T> Metaextension<T> use(String key, T value, T ignoreIfEqual) {
        return put(Extension.<T>key(key)).value(value, ignoreIfEqual);
    }

    default <T> Metaextension<T> literal(String key, String statement, Object... args) {
        return literal(Extension.key(key), statement, args);
    }

    default <T> Metaextension<T> literal(Extension<T> key, String statement, Object... args) {
        return literal(key, CodeBlock.builder().add(statement, args).build());
    }

    default <T> Metaextension<T> literal(String key, CodeBlock block) {
        return literal(Extension.key(key), block);
    }

    default <T> Metaextension<T> literal(Extension<T> key, CodeBlock block) {
        return put(key).valueBlock(block);
    }
}
