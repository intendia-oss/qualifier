package com.intendia.qualifier.example;

import static com.intendia.qualifier.example.ExampleModelExampleInner__.ExampleInnerMetadata;
import static com.intendia.qualifier.example.ExampleModel__.ExampleModelMetadata;
import static com.intendia.qualifier.example.ExampleModel__.stringListValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.intendia.qualifier.Extension;
import com.intendia.qualifier.Qualifier;
import com.intendia.qualifier.example.ExampleModel.Color;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.Assert;
import org.junit.Test;

public class ExampleProcessorExtensionTest {
    @Test public void test_processor_extensions_are_executed() {
        Object expectedType = ExampleModel.ExampleInnerInterface.class;
        assertNotNull(ExampleModel__.stringValue.data("simple.loaded"));
        assertEquals(1, ExampleModel__.stringValue.data("simple.integer"));
        assertEquals("s", ExampleModel__.stringValue.data("simple.string"));
        assertEquals(expectedType, ExampleModel__.stringValue.data("simple.type"));
        assertEquals("literal", ExampleModel__.stringValue.data("simple.literal"));
    }

    @Test public void assert_that_typed_extension_works() {
        ExampleManualExtension<String> q = ExampleManualExtension.of(ExampleModel__.stringValue);
        Class<?> expectedType = ExampleModel.ExampleInnerInterface.class;
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
        assertEquals(TimeUnit.SECONDS, q.data(Extension.<TimeUnit>key("extension.enum")));
        Assert.assertEquals(Color.valueOf("red"), q.data(Extension.<Color>key("extension.valueOf")));
        assertEquals(String.class, q.data(Extension.<Class<?>>key("extension.class")));
    }

    @Test public void test_works() throws Exception {
        assertNotNull(ExampleModelMetadata);
        assertNotNull(ExampleInnerMetadata);
        assertEquals(List.class, stringListValue.getType());
    }
}
