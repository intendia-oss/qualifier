package com.intendia.qualifier.processor;

import javax.lang.model.type.TypeMirror;

public interface QualifyExtensionData {
    String getKey();

    /** The extension type class (e.g. {@code Class<java.lang.String>}). */
    TypeMirror getType();

    /** The processor-time value for this extension. */
    Object getValue();

    <T> T getValue(Class<T> type);

    /**
     * Returns the literal representation for this extension value. E.g. a String literal returns {@code "extension
     * value"}, Integer literal returns {@code 123} and SomeType returns {@code SomeType.valueOf("extension value")}.
     */
    String toLiteral();
}
