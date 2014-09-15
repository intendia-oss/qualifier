package com.intendia.qualifier;

import com.google.gwt.cell.client.Cell;
import com.google.gwt.text.shared.Renderer;
import com.google.gwt.text.shared.SafeHtmlRenderer;
import java.util.Collections;
import java.util.Set;
import javax.annotation.Nullable;

public interface ResourceProvider {

    public Set<Class<?>> getSupportedTypes();

    interface TextRendererResourceProvider extends ResourceProvider {
        <V> Renderer<V> get(@Nullable Qualifier<?, V> qualifier);
    }

    interface HtmlRendererResourceProvider extends ResourceProvider {
        <V> SafeHtmlRenderer<V> get(@Nullable Qualifier<?, V> qualifier);
    }

    interface CellRendererResourceProvider extends ResourceProvider {
        <V> Cell<V> get(@Nullable Qualifier<?, V> qualifier);
    }

    public static abstract class AbstractResourceProvider implements ResourceProvider {
        @Override
        public Set<Class<?>> getSupportedTypes() {
            return Collections.emptySet(); // empty set means support all types
        }
    }
}
