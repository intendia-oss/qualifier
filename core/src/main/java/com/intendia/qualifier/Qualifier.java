// Copyright 2015 Intendia, SL.
package com.intendia.qualifier;

import static java.util.Collections.emptySet;

import java.util.Collection;
import javax.annotation.Nullable;

@FunctionalInterface
@SuppressWarnings("ClassReferencesSubclass")
public interface Qualifier<V> extends Metadata {
    String CORE_NAME_KEY = "core.name";
    String CORE_TYPE_KEY = "core.type";
    String CORE_GENERICS_KEY = "core.generics";
    String CORE_PROPERTIES_KEY = "core.properties";
    Extension<String> CORE_NAME = Extension.key(CORE_NAME_KEY);
    Extension<Class<?>> CORE_TYPE = Extension.key(CORE_TYPE_KEY);
    Extension<Class<?>[]> CORE_GENERICS = Extension.key(CORE_GENERICS_KEY);
    Extension<Collection<? extends PropertyQualifier<?, ?>>> CORE_PROPERTIES = Extension.key(CORE_PROPERTIES_KEY);
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
                return split.length == 1 ? property : property.compose(split[1]);
            }
        }
        return null;
    }

    default Qualifier<V> overrideQualifier() {
        return Metadata.override(this, Qualifier::unchecked);
    }

    @SuppressWarnings("unchecked")
    static <V> Qualifier<V> unchecked(Metadata q) {
        return q instanceof Qualifier ? (Qualifier<V>) q : q::data;
    }
}
