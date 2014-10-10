// Copyright 2014 Intendia, SL.
package com.intendia.qualifier.example.gwt;

import static com.intendia.qualifier.BaseQualifier.QualifierResolver;
import static com.intendia.qualifier.PropertyQualifier.GETTER;
import static com.intendia.qualifier.PropertyQualifier.PropertyGetter;
import static com.intendia.qualifier.PropertyQualifier.PropertySetter;
import static com.intendia.qualifier.Qualifier.QualifierResolver;
import static com.intendia.qualifier.Qualifier.StaticQualifierContext;
import static com.intendia.qualifier.example.gwt.View.AddressQualifier.AddressExtendedQualifier;
import static com.intendia.qualifier.example.gwt.View.AddressQualifier.AddressPropertyQualifier;
import static com.intendia.qualifier.example.gwt.View.AddressQualifier.STREET;
import static com.intendia.qualifier.example.gwt.View.PersonQualifier.ADDRESS;
import static com.intendia.qualifier.example.gwt.View.PersonQualifier.PersonAddressQualifier;
import static com.intendia.qualifier.example.gwt.View.PersonQualifier.PersonPropertyQualifier;
import static com.intendia.qualifier.example.gwt.View.PersonQualifier.personQualifier;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableMap;
import com.google.gwt.text.shared.Renderer;
import com.intendia.qualifier.PropertyQualifier;
import com.intendia.qualifier.Qualifier;
import com.intendia.qualifier.QualifierContext;
import com.intendia.qualifier.QualifierManager;
import java.util.Comparator;

@SuppressWarnings("UnusedDeclaration")
public class View {
    void usage() {
        final Person somePerson = mock(Person.class);
        final Address someAddress = mock(Address.class);
        when(somePerson.getAddress()).thenReturn(someAddress);
        when(someAddress.getStreet()).thenReturn("expected");
        // Static strategy
        final QualifierManager manager = null;
        final PersonQualifier person = personQualifier();

        final PersonPropertyQualifier<Address> address = ADDRESS;
        final Address address_getter = address.get(somePerson);
        final String street_getter = STREET.get(address_getter);
        final String street_chain = STREET.get(address.get(somePerson));
        //final PropertyQualifier<PersonPropertyQualifier<Address>, Address> traverse = AddressQualifier.street.traverse(address);
//        final PropertyQualifier<PropertyQualifier<Person, Address>, Address> traverse = AddressQualifier.street.traverse(address);
        final PropertyQualifier<Address, String> _street = STREET;
        final PropertyQualifier<Person, Address> _address = ADDRESS;
        final PropertyQualifier<Person, String> traverse = _street.of(_address);
        assertEquals("street", traverse.getName());
        assertEquals("address", traverse.getPath());
        final String street_traverse = traverse.get(somePerson);



        // path and flat
        final AddressPropertyQualifier<Person, String> typed_traverse = STREET.of(ADDRESS);
        assertEquals("expected", typed_traverse.get(somePerson));
        assertEquals("expected", typed_traverse.getTextRenderer().render("expected"));

        final AddressQualifier<Person> flat_traverse = STREET.of(ADDRESS).flat();
        // assertEquals(somePerson, flat_traverse.get(somePerson)); illegal
        assertEquals("expected", flat_traverse.getTextRenderer().render(somePerson));





        // PropertyQualifier<Person, Street> path = path(ADDRESS, STREET)
        // path.as(RendererExtension.class)
        // QualifierManagerFactory.instance().path(ADDRESS, STREET).as(RendererExtension.class)
        final String personStreet_traverse = typed_traverse.get(somePerson);
        final Renderer<String> textRenderer1 = typed_traverse.getTextRenderer();
        final Renderer<Person> textRenderer2 = typed_traverse.flat().getTextRenderer();


//        Qualifiers.path(personQualifier(), ADDRESS, STREET)

        final Renderer<Person> streetRenderer = STREET.of(ADDRESS).flat().getTextRenderer();
        final PropertyGetter<Person, String> streetGetter = STREET.of(ADDRESS).getGetter();
        assertEquals("street", STREET.of(ADDRESS).getName());
        assertEquals("address", STREET.of(ADDRESS).getPath());
        assertEquals("street", STREET.of(ADDRESS).flat().getName());
        assertEquals("person.address", STREET.of(ADDRESS).flat().getPath());

        // FactoryBuilder.of(Person.class)
        //   .add(ADDRESS)
        //   .add(STREET.map(ADDRESS))
        //   .build();


        // AddressQualifier.street.as(PersonQualifier.address)


        // AddressQualifier.street.as(

        final Renderer<String> textRenderer = STREET.of(ADDRESS).getTextRenderer();

        final Qualifier<Address> address1 = ADDRESS;
        final PropertyQualifier<PersonQualifier, Person> as2 = address.as(person);
        final PersonPropertyQualifier<Person> personAsAddress = as2;

        final AddressPropertyQualifier<Address> as1 = STREET.of(address);
        final PersonPropertyQualifier<Person> personAsStreet = as1.of(person);

        // PersonPropertyQualifier<Person> = traverse(person,address);
        // AddressPropertyQualifier<Person> = traverse(person,address,street);
        // AddressPropertyQualifier<Address> = traverse(address,street);

        // PersonPropertyQualifier<Person> = address.as(person);
        // AddressPropertyQualifier<Person> = street.as(address).as(person);
        // AddressPropertyQualifier<Address> = street.as(address);

        final Renderer<Address> addressRenderer = address.getTextRenderer();
        final Renderer<Person> personAsAddressRenderer = personAsAddress.getTextRenderer();

        // Alternative
        final Renderer<Address> addressRenderer = PersonMetamodel.PersonMetadata.address().getTextRenderer();

        // Chained traverse of qualifiers
        Comparator<? super Person> personComparator = person.getComparator(); // person comparator by person
        Comparator<? super Address> addressComparator = address.getComparator(); // address comparator by address
        AddressQualifier as = person.as(address);
        PersQaddress.as(person)
        Comparator<Person> personAddressComparator = as.getComparator(); // chained!
        Renderer<Person> person.as(address).getTextRender();


        // RendererProvider<Person> rendererProvider = manager
        // .getResourceProvider(RendererProvider.class, person.address());


        // final Renderer<Person> personAddressRenderer = manager.createRenderer(person.address());

        // Dynamic strategy
        final BeanQualifier<Person> personQualifier = manager.getBeanQualifier(Person.class);
        final Qualifier<Person, String> personNameQualifier = personQualifier.as(PersonMetamodel.name);
    }


    @SuppressWarnings({"StaticVariableOfConcreteClass", "ClassReferencesSubclass"})
    public static final class PersonQualifier {
        private PersonQualifier(){}

        private static final QualifierContext CTX_PERSON =  StaticQualifierContext.of(new QualifierResolver() {
            @Override public Object resolve(String extensionKey) {
                switch (extensionKey) {
                    case Qualifier.CORE_NAME: return "person";
                    case Qualifier.CORE_TYPE: return Person.class;
                    case Qualifier.CORE_PROPERTIES: return ImmutableMap.builder().put(ID.getName(), ID).put(ADDRESS.getName(), ADDRESS).build();
                    case PropertyQualifier.GETTER: return new PropertyGetter<Person, Integer>() { public Integer get(Person instance) { return instance.getId(); } };
                    case PropertyQualifier.BEAN: return INSTANCE;
                    default: return null;
                }
            }
        });

        private static final QualifierContext CTX_PERSON_ID =  StaticQualifierContext.of(new QualifierResolver() {
            @Override public Object resolve(String extensionKey) {
                switch (extensionKey) {
                    case Qualifier.CORE_NAME: return "id";
                    case Qualifier.CORE_TYPE: return Integer.class;
                    // case Qualifier.CORE_SUPER: return IntegerQualifier.INSTANCE;
                    case PropertyQualifier.GETTER: return new PropertyGetter<Person, Integer>() { public Integer get(Person instance) { return instance.getId(); } };
                    case PropertyQualifier.CHAIN: return new PropertyQualifier.QualifierChain<>(ID);
                    default: return null;
                }
            }
        });

        private static final QualifierContext CTX_PERSON_ADDRESS =  StaticQualifierContext.of(new QualifierResolver() {
            @Override public Object resolve(String extensionKey) {
                switch (extensionKey) {
                    case Qualifier.CORE_NAME: return "address";
                    case Qualifier.CORE_TYPE: return Address.class;
                    case Qualifier.CORE_SUPER: return AddressQualifier.INSTANCE;
                    case PropertyQualifier.GETTER: return new PropertyGetter<Person, Address>() { public Address get(Person instance) { return instance.getAddress(); } };
                    case PropertyQualifier.SETTER: return new PropertySetter<Person, Address>() { public void set(Person instance, Address value) { instance.setAddress(value); } };
                    case PropertyQualifier.BEAN: return INSTANCE;
                    default: return null;
                }
            }
        });

        public static Qualifier<Person> personQualifier() { return INSTANCE; } /* static import friendly */
        public static final Qualifier<Person> INSTANCE = Qualifier.of(CTX_PERSON);
        public static final PropertyQualifier<Person, Integer> ID = PropertyQualifier.of(CTX_PERSON_ID);
        public static final PropertyQualifier<Person, Address> ADDRESS = PropertyQualifier.of(CTX_PERSON_ADDRESS);
    }

    @SuppressWarnings("StaticVariableOfConcreteClass")
    public static class AddressQualifier<T> extends Qualifier<T> {
        protected AddressQualifier(QualifierContext qualifierContext) { super(qualifierContext); }

        public static final QualifierContext CTX_ADDRESS =  StaticQualifierContext.of(new QualifierResolver() {
            @Override public Object resolve(String extensionKey) {
                switch (extensionKey) {
                    case Qualifier.CORE_NAME: return "address";
                    case Qualifier.CORE_TYPE: return Address.class;
                    case Qualifier.CORE_PROPERTIES: return ImmutableMap.builder().put(STREET.getName(), STREET).put(STREET.getName(), STREET).build();
                    default: return null;
                }
            }
        });

        public static final QualifierContext CTX_ADDRESS_STREET =  StaticQualifierContext.of(new QualifierResolver() {
            @Override public Object resolve(String extensionKey) {
                switch (extensionKey) {
                    case Qualifier.CORE_NAME: return "street";
                    case Qualifier.CORE_TYPE: return String.class;
                    case PropertyQualifier.GETTER: return new PropertyGetter<Address, String>() { public String get(Address instance) { return instance.getStreet(); } };
                    case PropertyQualifier.BEAN: return INSTANCE;
                    default: return null;
                }
            }
        });

        public static Qualifier<Address> addressQualifier() { return INSTANCE; } /* static import friendly */
        public static final Qualifier<Address> INSTANCE = Qualifier.of(CTX_ADDRESS);
        public static final PropertyQualifier<Address, String> STREET = PropertyQualifier.of(CTX_ADDRESS_STREET);
    }
}
