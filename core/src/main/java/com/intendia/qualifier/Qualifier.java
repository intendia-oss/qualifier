// Copyright 2015 Intendia, SL.
package com.intendia.qualifier;

import static java.util.Collections.emptySet;

import java.util.Collection;
import javax.annotation.Nullable;

@FunctionalInterface
@SuppressWarnings("ClassReferencesSubclass")
public interface Qualifier<V> extends Metadata {
    Extension<String> CORE_NAME = Extension.key("core.name");
    Extension<Class<?>> CORE_TYPE = Extension.key("core.type");
    Extension<Class<?>[]> CORE_GENERICS = Extension.key("core.generics");
    Extension<Collection<? extends PropertyQualifier<?, ?>>> CORE_PROPERTIES = Extension.key("core.properties");
    Class<?>[] NO_GENERICS = new Class[0];

    default String getName() { return data(CORE_NAME); }

    default Class<V> getType() { return data(CORE_TYPE.as()); }

    default Class<?>[] getGenerics() { return data(CORE_GENERICS, NO_GENERICS); }

    /** Return the property qualifiers of the bean qualifier. */
    default Collection<PropertyQualifier<V, ?>> getProperties() { return data(CORE_PROPERTIES.as(), emptySet()); }

    default @Nullable PropertyQualifier<V, ?> getProperty(String name) {
        if (name.isEmpty()) throw new IllegalArgumentException("not empty name required");
        String[] split = name.split("\\.", 2);
        for (PropertyQualifier<V, ?> property : getProperties()) {
            if (split[0].equals(property.getName())) {
                System.out.println("resolving name=" + name + ", with property " + property);
                return split.length == 1 ? property : property.compose(split[1]);
            }
        }
        return null;
    }

    //XXX experimental: utility to override one extension value
    default <E> Qualifier<V> override(Extension<E> extension, E value) {
        return str -> extension.getKey().equals(str) ? value : data(extension);
    }
}
