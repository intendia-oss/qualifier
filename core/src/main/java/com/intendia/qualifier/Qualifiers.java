// Copyright 2013 Intendia, SL.
package com.intendia.qualifier;

import static com.google.common.base.MoreObjects.firstNonNull;
import static com.google.common.base.Suppliers.memoize;
import static com.google.common.collect.FluentIterable.from;
import static com.google.gwt.i18n.client.DateTimeFormat.getFormat;

import com.google.common.base.Function;
import com.google.common.base.Supplier;
import com.google.gwt.cell.client.AbstractCell;
import com.google.gwt.cell.client.Cell;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.text.shared.AbstractRenderer;
import com.google.gwt.text.shared.AbstractSafeHtmlRenderer;
import com.google.gwt.text.shared.Renderer;
import com.google.gwt.text.shared.SafeHtmlRenderer;
import java.util.Date;
import java.util.List;
import javax.annotation.Nullable;

public final class Qualifiers {

    /** The default key for all representations. See {@link #createDefaultRepresenter()}. */
    public static final String DEFAULT_REPRESENTER = "";
    public static final DateTimeFormat ISO_8601 = getFormat("yyyy-MM-dd");

    // Standard context keys
    public static final String CORE_NAME = "core.name";
    public static final String I18N_DESCRIPTION = "i18n.description";
    public static final String I18N_ABBREVIATION = "i18n.abbreviation";
    public static final String I18N_SUMMARY = "i18n.summary";
    public static final String REPRESENTER_TEXT_RENDERER = "representer.textRenderer";
    public static final String REPRESENTER_HTML_RENDERER = "representer.htmlRenderer";
    public static final String REPRESENTER_CELL = "representer.cell";
    public static final String MEASURE_UNIT_OF_MEASURE = "measure.unitOfMeasure";
    public static final String MEASURE_QUANTITY = "measure.quantity";

    private Qualifiers() {}

    public static String getString(Qualifier<?, ?> qualifier, String extensionName) {
        return (String) qualifier.getContext().get(extensionName);
    }

    public static String getString(Qualifier<?, ?> qualifier, String extensionName, String defaultValue) {
        return firstNonNull(getString(qualifier, extensionName), defaultValue);
    }

    public static Function<Qualifier<?, ?>, String> names() {
        return new Function<Qualifier<?, ?>, String>() {
            @Nullable
            @Override
            public String apply(@Nullable Qualifier<?, ?> input) {
                return input == null ? null : input.getName();
            }
        };
    }

    public static <T> Function<T, String> createDefaultRepresenter() {
        return new DefaultRepresenter<>();
    }

    static class DefaultRepresenter<T> implements Function<T, String> {
        @Override
        public String apply(@Nullable T input) {
            if (input == null) {
                return ""; // Null has no representation
            } else if (input instanceof String) {
                return (String) input;
            } else if (input instanceof Date) {
                return ISO_8601.format((Date) input);
            } else if (input instanceof Enum<?>) {
                return input.toString();
            } else {
                return input.toString();
            }
        }
    }

    public static <T> Renderer<T> rendererWrapper(Function<T, String> representer) {
        return new RendererWrapper<>(representer);
    }

    private static class RendererWrapper<T> extends AbstractRenderer<T> {
        private final Function<T, String> representer;

        private RendererWrapper(Function<T, String> representer) {
            this.representer = representer;
        }

        @Override
        public String render(T object) {
            return firstNonNull(representer.apply(object), "");
        }
    }

    public static <T> SafeHtmlRenderer<T> createDefaultSafeHtmlRenderer() {
        return new DefaultSafeHtmlRenderer<>();
    }

    private static class DefaultSafeHtmlRenderer<T> extends AbstractSafeHtmlRenderer<T> {
        @Override
        public SafeHtml render(T object) {
            if (object == null) {
                return SafeHtmlUtils.fromSafeConstant(""); // Null has no representation
            } else if (object instanceof String) {
                return SafeHtmlUtils.fromString((String) object);
            } else if (object instanceof Date) {
                return SafeHtmlUtils.fromString(ISO_8601.format((Date) object));
            } else {
                return SafeHtmlUtils.fromString(object.toString());
            }
        }
    }

    public static <T> SafeHtmlRenderer<T> safeHtmlRendererWrapper(Renderer<T> renderer) {
        return new SafeHtmlRendererWrapper<>(renderer);
    }

    private static class SafeHtmlRendererWrapper<T> extends AbstractSafeHtmlRenderer<T> {
        private final Renderer<T> renderer;

        SafeHtmlRendererWrapper(Renderer<T> renderer) {
            this.renderer = renderer;
        }

        @Override
        public SafeHtml render(T object) {
            return SafeHtmlUtils.fromString(renderer.render(object));
        }
    }

    public static <T> Cell<T> cellWrapper(SafeHtmlRenderer<T> safeHtmlRenderer) {
        return new CellWrapper<T>(safeHtmlRenderer);
    }

    static class CellWrapper<T> extends AbstractCell<T> {
        private final SafeHtmlRenderer<T> safeHtmlRenderer;

        CellWrapper(SafeHtmlRenderer<T> safeHtmlRenderer) {
            this.safeHtmlRenderer = safeHtmlRenderer;
        }

        @Override
        public void render(Context context, T value, SafeHtmlBuilder sb) {
            sb.append(safeHtmlRenderer.render(value));
        }
    }

    /** Return a wrapped qualifier which memoize the resource responses (renderer, safe html renderer, cell). */
    public static <T, V> Qualifier<T, V> createResourceMemoizeQualifier(final Qualifier<T, V> qualifier) {
        return new ForwardingQualifier<T, V>() {

            @Override
            protected Qualifier<T, V> delegate() {
                return qualifier;
            }

            private final Supplier<Renderer<V>> rendererLoadingSupplier = memoize(new Supplier<Renderer<V>>() {
                @Override
                public Renderer<V> get() {
                    return delegate().getRenderer();
                }
            });

            @Override
            public Renderer<V> getRenderer() {
                return rendererLoadingSupplier.get();
            }

            private final Supplier<SafeHtmlRenderer<V>> safeHtmlRendererLoadingSupplier = memoize(new Supplier<SafeHtmlRenderer<V>>() {
                @Override
                public SafeHtmlRenderer<V> get() {
                    return delegate().getSafeHtmlRenderer();
                }
            });

            @Override
            public SafeHtmlRenderer<V> getSafeHtmlRenderer() {
                return safeHtmlRendererLoadingSupplier.get();
            }

            private final Supplier<Cell<V>> cellLoadingSupplier = memoize(new Supplier<Cell<V>>() {
                @Override
                public Cell<V> get() {
                    return delegate().getCell();
                }
            });

            @Override
            public Cell<V> getCell() {
                return cellLoadingSupplier.get();
            }
        };
    }

    public static <T> List<Qualifier<? super T, ?>> createResourceMemoizeQualifiers(
            final Iterable<Qualifier<? super T, ?>> qualifiers) {
        return from(qualifiers).transform(new Function<Qualifier<? super T, ?>, Qualifier<? super T, ?>>() {
            @Override
            public Qualifier<? super T, ?> apply(Qualifier<? super T, ?> input) {
                return createResourceMemoizeQualifier(input);
            }
        }).toList();
    }

}
