// Copyright 2015 Intendia, SL.
package com.intendia.qualifier;

import com.google.gwt.text.client.IntegerRenderer;
import com.google.gwt.text.shared.AbstractRenderer;
import com.google.gwt.text.shared.Renderer;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/** Experimenting alternative strategies for ResourceLoading (see iGestion Codecs for more examples). */
public class ResourceLoadingStrategy {
    static {
        final TextRendererRegistry registry = new TextRendererRegistry();
        registry.registry(Object.class, new TextRendererFactory<Object>() {
            @Override
            public Renderer<Object> create(Qualifier<?, Object> property) {
                final String textForNull = Qualifiers.getString(property, "textForNull", "");
                return new AbstractRenderer<Object>() {
                    @Override
                    public String render(Object object) {
                        return object == null ? textForNull : Objects.toString(object);
                    }
                };
            }
        });
        registry.registry(Integer.class, new TextRendererFactory<Integer>() {
            @Override
            public Renderer<Integer> create(Qualifier<?, Integer> property) {
                return IntegerRenderer.instance();
            }
        });
        registry.registry(Integer.class, new TextRendererFactory<Integer>() {
            @Override
            public Renderer<Integer> create(Qualifier<?, Integer> property) {
                if (Qualifiers.getString(property, "role", "").equals("time")) return null;
                else return new AbstractRenderer<Integer>() {
                    @Override
                    public String render(Integer object) {
                        return new Date(object.longValue() * 1000l).toString();
                    }
                };
            }
        });
        // final TextRendererFactory<List<String>> factory = registry.factory(vehiclesParam.getType());
        // final Renderer<List<String>> resolve = registry.resolve(vehiclesParam);
    }

    static class TextRendererRegistry {
        private Map<Class<?>, TextRendererFactory<?>> factoryMap = new HashMap<>();

        public <T> void registry(Class<T> type, TextRendererFactory<? super T> factory) {
            factoryMap.put(type, factory);
        }

        @SuppressWarnings("unchecked")
        public <T> TextRendererFactory<T> factory(Class<T> type) {
            return (TextRendererFactory<T>) factoryMap.get(type);
        }

        public <T> Renderer<T> resolve(Qualifier<?, T> property) {
            return factory(property.getType()).create(property);
        }

    }

    interface TextRendererFactory<T> {
        Renderer<T> create(Qualifier<?, T> property);
    }
}
