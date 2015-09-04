package com.intendia.qualifier;

import static com.google.common.base.Preconditions.checkArgument;
import static com.intendia.qualifier.QualifierModule.getTextRendererMapBinder;

import com.google.gwt.core.client.GWT;
import com.google.gwt.inject.client.AbstractGinModule;
import com.google.gwt.inject.client.GinModules;
import com.google.gwt.inject.client.Ginjector;
import com.google.gwt.inject.client.multibindings.GinMapBinder;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.text.shared.AbstractRenderer;
import com.google.gwt.text.shared.Renderer;
import com.intendia.qualifier.ResourceProvider.TextRendererResourceProvider;

public class ResourceManagerGwtTest extends GWTTestCase {
    @Override
    public String getModuleName() {
        return "com.intendia.qualifier.Qualify";
    }

    static class MyModule extends AbstractGinModule {

        @Override
        protected void configure() {
            install(new QualifierModule());
            final GinMapBinder<String, TextRendererResourceProvider> mapBinder = getTextRendererMapBinder(binder());
            mapBinder.addBinding("simple").to(SimpleTextRendererProvider.class);
            mapBinder.addBinding("myRenderer").to(MyTextRendererProvider.class);
        }

        static class SimpleTextRendererProvider extends ResourceProvider.AbstractResourceProvider
                implements TextRendererResourceProvider {
            @Override
            public <T> Renderer<T> get(Qualifier<?, T> qualifier) {
                return new AbstractRenderer<T>() {
                    @Override
                    public String render(T object) {
                        return "simple " + object;
                    }
                };
            }
        }

        static class MyTextRendererProvider extends ResourceProvider.AbstractResourceProvider
                implements TextRendererResourceProvider {
            @Override
            @SuppressWarnings("unchecked")
            public <T> Renderer<T> get(Qualifier<?, T> qualifier) {
                final Class<T> type = qualifier == null ? (Class<T>) String.class : qualifier.getType();
                checkArgument(String.class.equals(type), "unsupported type {}", type);
                return (Renderer<T>) new AbstractRenderer<String>() {
                    @Override
                    public String render(String object) {
                        return "my renderer " + object;
                    }
                };
            }
        }
    }

    @GinModules(MyModule.class) interface MyGinjector extends Ginjector {
        ResourceManager getResourceManager();
    }

    public void testCreateRenderer() throws Exception {
        final ResourceManager manager = GWT.<MyGinjector>create(MyGinjector.class).getResourceManager();
        final Renderer<? super String> defaultRenderer = manager.createRenderer(null, "");
        assertEquals(defaultRenderer.render("passthrough"), "passthrough");
        final Renderer<? super String> s1 = manager.createRenderer(null, "simple");
        final Renderer<? super String> s2 = manager.createRenderer(null, "simple");
        assertNotSame(s1, s2); // must allow different instances
    }

    public void testConfigureRendererOnOtherModules() throws Exception {
        final ResourceManager manager = GWT.<MyGinjector>create(MyGinjector.class).getResourceManager();
        final Renderer<? super Object> renderer = manager.createRenderer(null, "myRenderer");
        assertEquals(renderer.render("is awesome"), "my renderer is awesome");
    }

}
