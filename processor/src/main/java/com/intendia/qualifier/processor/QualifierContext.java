// Copyright 2014 Intendia, SL.
package com.intendia.qualifier.processor;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.intendia.qualifier.annotation.QualifyExtension;
import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;
import javax.annotation.Nullable;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeMirror;

public class QualifierContext {
    private final Map<String, QualifyExtensionData> data = new TreeMap<>();
    private final TypeElement classRepresenter;
    private final ReflectionHelper helper;

    public QualifierContext(TypeElement classRepresenter, ReflectionHelper helper) {
        this.classRepresenter = classRepresenter;
        this.helper = helper;
    }

    public <T> T getOrThrow(Class<T> type, String key) { return getOrDefault(type, key, null); }

    public <T> T getOrDefault(Class<T> type, String key, T defaultValue) {
        final QualifyExtensionData first = data.get(key);
        return checkNotNull(first != null ? first.getValue(type) : defaultValue, "%s not found", key);
    }

    public <T> void doIfExists(Class<T> type, String key, Apply<T> apply) throws IOException {
        final QualifyExtensionData data = this.data.get(key);
        if (data != null) apply.apply(data.getValue(type));
    }

    public void putIfNotNull(String key, Object value) {
        Preconditions.checkArgument(!(value instanceof DataExtension), "use specific method instead!");
        if (value != null && !isEmpty(value)) put(key, value);
    }

    /** Recommended convention to add unique values for a type (ex. {@code putIfNotNull(RoundingMode.class, UP)}) */
    public <T> void putIfNotNull(Class<T> type, @Nullable T value) { putIfNotNull(type.getName(), value); }

    public QualifyExtensionData put(String key, Object value) { return put(new DataExtension(key, value)); }

    public QualifyExtensionData put(QualifyExtension annotation) { return put(new DataExtension(annotation)); }

    public QualifyExtensionData put(String key, DeclaredType type, String value) {
        return put(new DataExtension(key, type, value));
    }

    public QualifyExtensionData putClass(String key, String className) {
        return put(key, helper.classType, className);
    }

    public QualifyExtensionData putLiteral(String key, String literalValue) {
        return put(new LiteralExtension(key, literalValue));
    }

    private QualifyExtensionData put(QualifyExtensionData extensionData) {
        data.put(extensionData.getKey(), extensionData);
        return extensionData;
    }

    private boolean isEmpty(Object value) { return value instanceof String && ((String) value).trim().isEmpty(); }

    public boolean contains(String key) { return data.containsKey(key); }

    public Iterable<QualifyExtensionData> getExtensions() { return data.values(); }

    public interface Apply<T> {
        void apply(T input) throws IOException;
    }

    public static TypeMirror loadType(QualifyExtension annotation) {
        // http://blog.retep.org/2009/02/13/getting-class-values-from-annotations-in-an-annotationprocessor/
        try {
            annotation.type();
            return null; // this must not happens
        } catch (MirroredTypeException exception) {
            return exception.getTypeMirror();
        }
    }

    private class LiteralExtension implements QualifyExtensionData {
        private final String key, literal;

        private LiteralExtension(String key, String literal) {
            this.key = key;
            this.literal = literal;
        }

        @Override public String getKey() { return key; }

        @Override public String toLiteral() { return literal; }

        @Override public TypeMirror getType() { return unsupported("type"); }

        @Override public Object getValue() { return unsupported("value"); }

        @Override public <T> T getValue(Class<T> type) { return unsupported("value"); }

        private <T> T unsupported(String value) {
            throw new UnsupportedOperationException("literal extensions has no processor-time " + value);
        }
    }

    private class DataExtension implements QualifyExtensionData {
        private final String key;
        private final TypeMirror type;
        private final Object value;
        private final String castValue;

        private DataExtension(QualifyExtension annotation) {
            this(annotation.key(), loadType(annotation), annotation.value());
        }

        public DataExtension(String key, Object value) {
            this(key, helper.getTypeMirror(value.getClass()), value);
        }

        private DataExtension(String key, TypeMirror type, Object value) {
            Preconditions.checkArgument(!(value instanceof DataExtension), "please, be careful!");
            this.key = checkNotNull(key, "requires non null keys");
            this.type = type;
            this.value = checkNotNull(value, "requires non null values");

            final String typeString = Splitter.on('<').split(type.toString()).iterator().next();
            switch (typeString) { //@formatter:off
                case "java.lang.Class": castValue = value + ".class"; break;
                case "java.lang.String": castValue = "\"" + value + "\""; break;
                case "java.lang.Integer": castValue = value.toString(); break;
                default: castValue = typeString + ".valueOf(\"" + value + "\")";
            } //@formatter:on
        }

        @Override public String getKey() { return key; }

        @Override public TypeMirror getType() { return type; }

        @Override public Object getValue() { return value; }

        @Override public <T> T getValue(Class<T> type) {
            Preconditions.checkArgument(getType().toString().equals(type.getName()),
                    "value type mismatch (class: %s, key: %s, value: %s):  expected type %s, actual type %s",
                    classRepresenter, key, getValue(), type.getName(), getType());
            return type.cast(getValue());
        }

        @Override public String toLiteral() { return castValue; }
    }
}
