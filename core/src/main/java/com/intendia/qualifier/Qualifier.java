package com.intendia.qualifier;

import static java.util.Collections.emptySet;

import java.util.Collection;
import java.util.Comparator;
import java.util.function.Consumer;
import java.util.function.Function;
import javax.annotation.Nullable;

@FunctionalInterface
@SuppressWarnings("ClassReferencesSubclass")
public interface Qualifier<T> extends Metadata {
    String CORE_NAME_KEY = "core.name";
    String CORE_TYPE_KEY = "core.type";
    String CORE_GENERICS_KEY = "core.generics";
    String CORE_PROPERTIES_KEY = "core.properties";
    String COMPARABLE_COMPARATOR_KEY = "comparable.comparator";
    Extension<String> CORE_NAME = Extension.key(CORE_NAME_KEY);
    Extension<Class<?>> CORE_TYPE = Extension.key(CORE_TYPE_KEY);
    Extension<Class<?>[]> CORE_GENERICS = Extension.key(CORE_GENERICS_KEY);
    Extension<Collection<? extends PropertyQualifier<?, ?>>> CORE_PROPERTIES = Extension.key(CORE_PROPERTIES_KEY);
    Extension<Comparator<?>> COMPARABLE_COMPARATOR = Extension.key(COMPARABLE_COMPARATOR_KEY);
    Class<?>[] NO_GENERICS = new Class[0];

    default String getName() { return req(CORE_NAME); }

    default Class<T> getType() { return req(CORE_TYPE.as()); }

    default Class<?>[] getGenerics() { return data(CORE_GENERICS, NO_GENERICS); }

    /** Return the property qualifiers of the bean qualifier. */
    default Collection<PropertyQualifier<T, ?>> getProperties() { return data(CORE_PROPERTIES.as(), emptySet()); }

    default @Nullable PropertyQualifier<T, ?> getProperty(String name) {
        if (name.isEmpty()) throw new IllegalArgumentException("not empty name required");
        String[] split = name.split("\\.", 2);
        for (PropertyQualifier<T, ?> property : getProperties()) {
            if (split[0].equals(property.getName())) {
                return split.length == 1 ? property : property.compose(split[1]);
            }
        }
        return null;
    }

    default Comparator<T> getTypeComparator() {
        return data(COMPARABLE_COMPARATOR.as(), toStringComparator());
    }

    default <F> Comparator<F> orderingOnResultOf(Function<F, ? extends T> function) {
        return Comparator.comparing(function, getTypeComparator());
    }

    default Qualifier<T> overrideQualifier() { return unchecked(override()); }

    default Qualifier<T> overrideQualifier(Consumer<Mutadata> fn) { return unchecked(override(fn)); }

    @SuppressWarnings("unchecked")
    static <T> Qualifier<T> unchecked(Metadata q) {
        return q instanceof Qualifier ? (Qualifier<T>) q : q::data;
    }

    static <T> Qualifier<T> create(Class<T> type) { return create(type, type.getSimpleName()); }

    static <T> Qualifier<T> create(Class<T> type, String name) {
        return key -> {
            switch (key) {
                case CORE_NAME_KEY: return name;
                case CORE_TYPE_KEY: return type;
                default: return null;
            }
        };
    }

    @SuppressWarnings("unchecked")
    static <T extends Comparable<T>> Comparator<T> naturalComparator() {
        return (Comparator<T>) Defaults.NATURAL_COMPARATOR;
    }

    @SuppressWarnings("unchecked")
    static <T> Comparator<T> toStringComparator() {
        return (Comparator<T>) Defaults.TO_STRING_COMPARATOR;
    }
}

final class Defaults {
    static Comparator<?> TO_STRING_COMPARATOR = Comparator.nullsFirst(Comparator.comparing(Object::toString));
    static Comparator<?> NATURAL_COMPARATOR = Comparator.nullsFirst(Comparator.naturalOrder());
}
