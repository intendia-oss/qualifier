package com.intendia.qualifier.processor;

import static com.google.common.base.Preconditions.checkArgument;

import com.intendia.qualifier.Extension;
import com.squareup.javapoet.CodeBlock;
import java.util.Optional;
import javax.annotation.Nullable;
import javax.lang.model.type.TypeMirror;

public interface Metaextension<T> {
    Extension<T> extension();

    /** The extension type class (e.g. {@code Class<java.lang.String>}). */
    Optional<TypeMirror> type();

    Metaextension<T> type(TypeMirror type);

    Metaextension<T> type(Class<T> type);

    /** The processor-time value for this extension. Might not be available. */
    Optional<T> value();

    Metaextension<T> value(T value);

    Metaextension<T> value(T value, T ignoreIfEqual);

    /**
     * Returns the literal representation for this extension value. E.g. a String literal returns {@code "extension
     * value"}, Integer literal returns {@code 123} and SomeType returns {@code SomeType.valueOf("extension
     * value")}.
     */
    Optional<CodeBlock> valueBlock();

    @SuppressWarnings("unchecked") default <V> Metaextension<V> as(Extension<V> other) {
        checkArgument(other == extension(), "extension mismatch, expected %s but was %s", extension(), other);
        return (Metaextension<V>) this;
    }

    Metaextension<T> valueBlock(@Nullable CodeBlock block);

    default Metaextension<T> valueBlock(String format, Object... args) {
        return valueBlock(CodeBlock.builder().add(format, args).build());
    }

    Metaqualifier done();
}
