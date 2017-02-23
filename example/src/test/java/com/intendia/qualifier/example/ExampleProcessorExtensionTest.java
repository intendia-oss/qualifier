package com.intendia.qualifier.example;

import static com.intendia.qualifier.ComparableQualifier.COMPARABLE_COMPARATOR;
import static com.intendia.qualifier.example.ExampleModelExampleInner__.ExampleInnerMetadata;
import static com.intendia.qualifier.example.ExampleModel__.ExampleModelMetadata;
import static com.intendia.qualifier.example.ExampleModel__.stringListValue;
import static java.util.Objects.requireNonNull;
import static java.util.concurrent.TimeUnit.DAYS;
import static java.util.concurrent.TimeUnit.HOURS;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.intendia.qualifier.ComparableQualifier;
import com.intendia.qualifier.Extension;
import com.intendia.qualifier.PropertyQualifier;
import com.intendia.qualifier.Qualifier;
import com.intendia.qualifier.example.ExampleModel.ExampleInnerInterface;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.Assert;
import org.junit.Test;

public class ExampleProcessorExtensionTest {
    @Test public void test_processor_extensions_are_executed() {
        Object expectedType = ExampleInnerInterface.class;
        assertNotNull(ExampleModel__.stringValue.data("simple.loaded"));
        assertEquals(1, ExampleModel__.stringValue.data("simple.integer"));
        assertEquals("s", ExampleModel__.stringValue.data("simple.string"));
        assertEquals(expectedType, ExampleModel__.stringValue.data("simple.type"));
        assertEquals("literal", ExampleModel__.stringValue.data("simple.literal"));
    }

    @Test public void assert_that_typed_extension_works() {
        ExampleManualExtension<String> q = ExampleManualExtension.of(ExampleModel__.stringValue);
        Class<?> expectedType = ExampleInnerInterface.class;
        assertNotNull(ExampleModel__.stringValue.data("simple.loaded"));
        assertEquals(Integer.valueOf(1), q.getExampleInteger());
        assertEquals("s", q.getExampleString());
        assertEquals(expectedType, q.getExampleType());
    }

    @Test public void assert_that_qualifier_extension_works() {
        Qualifier<ExampleModel> q = ExampleModelMetadata;
        assertEquals("string value", q.data(Extension.<String>key("extension.string")));
        assertEquals(true, q.data(Extension.<Boolean>key("extension.boolean")));
        assertEquals(Integer.valueOf(1), q.data(Extension.<Integer>key("extension.int")));
        assertEquals(SECONDS, q.data(Extension.<TimeUnit>key("extension.enum")));
        Assert.assertEquals(Color.valueOf("red"), q.data(Extension.<Color>key("extension.valueOf")));
        assertEquals(String.class, q.data(Extension.<Class<?>>key("extension.class")));
    }

    @Test public void assert_that_auto_qualifier_values_works() {
        ExampleAutoQualifier q = ExampleModel__.stringValue;
        assertEquals(SECONDS, q.data("exampleAuto.enumeration"));
        assertEquals(1, q.data("exampleAuto.integer"));
        assertEquals("s", q.data("exampleAuto.string"));
        assertEquals(ExampleInnerInterface.class, q.data("exampleAuto.type"));
        assertEquals(Color__.self, q.data("exampleAuto.link"));
    }

    @Test public void assert_that_auto_qualifier_defaults_works() {
        ExampleAutoQualifier q = ExampleAutoQualifier.of(key -> null);
        assertEquals(null, q.data("exampleAuto.enumeration"));
        assertEquals(MILLISECONDS, q.getExampleAutoEnumeration());
        assertEquals(null, q.data("exampleAuto.integer"));
        assertEquals(Integer.valueOf(-1), q.getExampleAutoInteger());
        assertEquals(null, q.data("exampleAuto.string"));
        assertEquals("def", q.getExampleAutoString());
        assertEquals(null, q.data("exampleAuto.type"));
        assertEquals(Void.class, q.getExampleAutoType());
        assertArrayEquals(new TimeUnit[0], q.getExampleAutoEnumerationList());
        assertArrayEquals(new TimeUnit[] { DAYS, HOURS }, q.getExampleAutoEnumerationListWithDefaults());
    }

    @Test public void test_works() throws Exception {
        assertNotNull(ExampleModelMetadata);
        assertNotNull(ExampleInnerMetadata);
        assertEquals(List.class, stringListValue.getType());
    }

    @Test public void assert_comparator_can_be_override() {
        Comparator<ExampleModel> stringComparator = ExampleModel__.stringValue.getPropertyComparator();
        Qualifier<ExampleModel> override = ExampleModelMetadata.overrideQualifier();
        override.mutate().put(COMPARABLE_COMPARATOR, stringComparator);
        // this is easy, just confirm comparable returns the override qualifier
        assertEquals(stringComparator, ComparableQualifier.of(override).getTypeComparator());
        // this is the important, confirm that identity decorator maintains the override comparator
        assertEquals(stringComparator, PropertyQualifier.asProperty(override).getPropertyComparator());
    }

    @Test public void assert_paths() {
        PropertyQualifier<ExampleModel, ExampleModel> qSelf = PropertyQualifier.asProperty(ExampleModelMetadata);
        assertEquals("", qSelf.getPath());
        assertEquals("self", qSelf.getName());
        assertEquals("override", qSelf.getPath("override"));

        PropertyQualifier<ExampleModel, String> qString = ExampleModel__.stringValue;
        assertEquals("stringValue", qString.getPath());
        assertEquals("stringValue", qString.getName());
        assertEquals("override", qString.getPath("override"));

        PropertyQualifier<ExampleModel, String> qColor = ExampleModel__.colorValue.compose(Color__.name);
        assertEquals("colorValue.name", qColor.getPath());
        assertEquals("name", qColor.getName());
        assertEquals("colorValue.override", qColor.getPath("override"));
    }

    @Test public void assert_property_resolution_works() {
        Qualifier<ExampleModel> q = ExampleModelMetadata;
        assertEquals("colorValue", requireNonNull(q.getProperty("colorValue")).getPath());
        assertEquals("colorValue.name", requireNonNull(q.getProperty("colorValue.name")).getPath());
    }

    @Test public void mutators_api_looks_good() {
        PropertyQualifier<ExampleModel, String> q = ExampleModel__.stringValue;
        assertEquals(Integer.valueOf(1), q.data(ExampleAutoQualifier.EXAMPLE_AUTO_INTEGER));

        assertEquals(Integer.valueOf(2), PropertyQualifier
                .unchecked(ExampleModel__.stringValue.override())
                .mutate()
                .put(ExampleAutoQualifier.EXAMPLE_AUTO_INTEGER, 2)
                .data(ExampleAutoQualifier.EXAMPLE_AUTO_INTEGER));

        PropertyQualifier<ExampleModel, String> mutable = ExampleModel__.stringValue.overrideProperty();
        PropertyQualifier<ExampleModel, String> nested = mutable.overrideProperty();

        mutable.mutate().put(ExampleAutoQualifier.EXAMPLE_AUTO_INTEGER, 3);
        assertEquals(Integer.valueOf(3), mutable.data(ExampleAutoQualifier.EXAMPLE_AUTO_INTEGER));

        nested.mutate().put(ExampleAutoQualifier.EXAMPLE_AUTO_INTEGER, 4);
        assertEquals(Integer.valueOf(4), nested.data(ExampleAutoQualifier.EXAMPLE_AUTO_INTEGER));

        nested.mutate().remove(ExampleAutoQualifier.EXAMPLE_AUTO_INTEGER);
        assertEquals(Integer.valueOf(3), nested.data(ExampleAutoQualifier.EXAMPLE_AUTO_INTEGER));
    }

    @Test public void mixins_usage() {
        // expected values defined in ExampleMixin instead of ExampleModel
        assertEquals("mixin", ExampleModel__.stringValue.data("mixin.val"));
        assertEquals("mixin", ExampleModel__.categoryString.data("mixin.val"));
    }
}
