package com.intendia.qualifier;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.gwt.inject.client.multibindings.GinMapBinder.newMapBinder;
import static com.intendia.qualifier.Qualifiers.REPRESENTER_TEXT_RENDERER;

import com.google.gwt.cell.client.Cell;
import com.google.gwt.inject.client.AbstractGinModule;
import com.google.gwt.inject.client.binder.GinBinder;
import com.google.gwt.inject.client.multibindings.GinMapBinder;
import com.google.gwt.text.shared.Renderer;
import com.google.gwt.text.shared.SafeHtmlRenderer;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.intendia.qualifier.ResourceProvider.AbstractResourceProvider;
import com.intendia.qualifier.ResourceProvider.CellRendererResourceProvider;
import com.intendia.qualifier.ResourceProvider.HtmlRendererResourceProvider;
import com.intendia.qualifier.ResourceProvider.TextRendererResourceProvider;
import javax.annotation.Nullable;

public class QualifierModule extends AbstractGinModule {

    @Override
    protected void configure() {
        bind(ResourceManager.class).to(ResourceManager.StandardResourceManager.class);
        install(new DefaultResources());
    }

    static class DefaultResources extends AbstractGinModule {

        @Override
        protected void configure() {
            requestStaticInjection(BaseQualifier.class);
            getTextRendererMapBinder(binder()).addBinding("").to(DefaultTextRendererProvider.class);
            getHtmlRendererMapBinder(binder()).addBinding("").to(DefaultHtmlRendererProvider.class);
            getCellMapBinder(binder()).addBinding("").to(DefaultCellRendererProvider.class);
        }

        @Singleton
        static class DefaultTextRendererProvider extends AbstractResourceProvider
                implements TextRendererResourceProvider {
            @Override
            public <T> Renderer<T> get(@Nullable Qualifier<?, T> qualifier) {
                return Qualifiers.rendererWrapper(Qualifiers.<T> createDefaultRepresenter());
            }
        }

        @Singleton
        static class DefaultHtmlRendererProvider extends AbstractResourceProvider
                implements HtmlRendererResourceProvider {
            @Override
            public <T> SafeHtmlRenderer<T> get(@Nullable Qualifier<?, T> qualifier) {
                if (qualifier == null || isNullOrEmpty(Qualifiers.getString(qualifier, REPRESENTER_TEXT_RENDERER))) {
                    return Qualifiers.createDefaultSafeHtmlRenderer();
                } else {
                    return Qualifiers.safeHtmlRendererWrapper(checkNotNull(qualifier).getRenderer());
                }
            }
        }

        @Singleton
        static class DefaultCellRendererProvider extends AbstractResourceProvider
                implements CellRendererResourceProvider {
            @Override
            public <T> Cell<T> get(@Nullable Qualifier<?, T> qualifier) {
                if (qualifier == null) return Qualifiers.cellWrapper(Qualifiers.<T> createDefaultSafeHtmlRenderer());
                else return Qualifiers.cellWrapper(qualifier.getSafeHtmlRenderer());
            }
        }
    }

    public static GinMapBinder<String, TextRendererResourceProvider> getTextRendererMapBinder(GinBinder binder) {
        return newMapBinder(binder, new TypeLiteral<String>() {}, new TypeLiteral<TextRendererResourceProvider>() {});
    }

    public static GinMapBinder<String, HtmlRendererResourceProvider> getHtmlRendererMapBinder(GinBinder binder) {
        return newMapBinder(binder, new TypeLiteral<String>() {}, new TypeLiteral<HtmlRendererResourceProvider>() {});
    }

    public static GinMapBinder<String, CellRendererResourceProvider> getCellMapBinder(GinBinder binder) {
        return newMapBinder(binder, new TypeLiteral<String>() {}, new TypeLiteral<CellRendererResourceProvider>() {});
    }

}
