// Copyright 2014 Intendia, SL.
package com.intendia.qualifier.example.gwt;

import static com.intendia.qualifier.example.gwt.Person__.PersonMetadata;

import com.google.gwt.text.shared.Renderer;
import com.intendia.qualifier.BeanQualifier;
import com.intendia.qualifier.Qualifier;
import com.intendia.qualifier.QualifierManager;

public class View {
    void usage() {
        QualifierManager manager = null;
        // Static strategy
        final BeanQualifier<Person> self = Person__.self;
        final Qualifier<Person, String> name = Person__.name;
        final Renderer<? super String> renderer = name.getRenderer();
        final Renderer<? super String> renderer1 = self.as(name).getRenderer(); // must return Renderer<Person>

        // Alternative
        final Renderer<Address> addressRenderer = PersonMetadata.address().getRenderer();

        // RendererProvider<Person> rendererProvider = manager
        // .getResourceProvider(RendererProvider.class, person.address());


        // final Renderer<Person> personAddressRenderer = manager.createRenderer(person.address());

        // Dynamic strategy
        final BeanQualifier<Person> personQualifier = manager.getBeanQualifier(Person.class);
        final Qualifier<Person, String> personNameQualifier = personQualifier.as(Person__.name);
    }

}
