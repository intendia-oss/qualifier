package com.intendia.qualifier;

import static java.util.Objects.requireNonNull;

import com.intendia.qualifier.Metadata.Mutadata;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import javax.annotation.Nullable;

@FunctionalInterface
public interface Metadata {
    String METADATA_MUTATOR_KEY = "metadata.mutator";
    Extension<Mutadata> METADATA_MUTATOR = Extension.key(METADATA_MUTATOR_KEY);

    @Nullable Object data(String key);

    @SuppressWarnings("unchecked")
    default @Nullable <T> T data(Extension<T> key) {
        return (T) data(key.getKey());
    }

    default <T> T data(Extension<T> key, T or) {
        T v = data(key); return v != null ? v : or;
    }

    default <T> T data(Extension<T> key, Supplier<T> or) {
        T v = data(key); return v != null ? v : or.get();
    }

    default <T> Optional<T> opt(Extension<T> key) {
        return Optional.ofNullable(data(key));
    }

    default <T> T req(Extension<T> key) {
        return requireNonNull(data(key), key + " missing");
    }

    /** Return the nearest mutable metadata or throws NPE if no mutable column exists. */
    default Mutadata mutate() {
        return this instanceof Mutadata ? (Mutadata) this : requireNonNull(data(METADATA_MUTATOR), "non mutable");
    }

    /** Creates a new mutable metadata to allow overrides values on this metadata. */
    default Mutadata override() {
        return new HashMutadata(this);
    }

    default Metadata override(Consumer<Mutadata> fn) {
        Mutadata m = override(); fn.accept(m); return m;
    }

    static <T extends Metadata> T override(T ref, Function<Metadata, T> cast) {
        return cast.apply(ref.override());
    }

    static Mutadata create() {
        return new HashMutadata(null);
    }

    interface Mutadata extends Metadata {

        Mutadata put(String key, @Nullable Object value);

        default <V> Mutadata put(Extension<V> extension, @Nullable V value) {
            return put(extension.getKey(), value);
        }

        Mutadata remove(String key);

        default Mutadata remove(Extension<?> extension) {
            return remove(extension.getKey());
        }
    }
}

class HashMutadata implements Mutadata {
    final @Nullable Metadata parent;
    final Map<String, Object> data = new HashMap<>();

    HashMutadata(@Nullable Metadata parent) {
        this.parent = parent;
    }

    @Override public @Nullable Object data(String key) {
        if (METADATA_MUTATOR_KEY.equals(key)) return this;
        return parent == null || data.containsKey(key) ? data.get(key) : parent.data(key);
    }

    @Override public Mutadata put(String key, @Nullable Object value) {
        data.put(key, value);
        return this;
    }

    @Override public Mutadata remove(String key) {
        data.remove(key);
        return this;
    }
}
