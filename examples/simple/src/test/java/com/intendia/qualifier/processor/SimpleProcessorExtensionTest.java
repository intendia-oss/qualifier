package com.intendia.qualifier.processor;

import static com.intendia.qualifier.processor.SimpleModelSimpleInner__.SimpleInnerMetadata;
import static com.intendia.qualifier.processor.SimpleModel__.SimpleModelMetadata;
import static com.intendia.qualifier.processor.SimpleModel__.stringListValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.intendia.qualifier.Extension;
import com.intendia.qualifier.Qualifier;
import com.intendia.qualifier.processor.SimpleModel.Color;
import com.intendia.qualifier.processor.SimpleModel.SimpleInnerInterface;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.Test;

public class SimpleProcessorExtensionTest {
    @Test public void test_processor_extensions_are_executed() {
        final Object expectedType = SimpleInnerInterface.class;
        assertNotNull(SimpleModel__.simpleValue.data("simple.loaded"));
        assertEquals(1, SimpleModel__.simpleValue.data("simple.integer"));
        assertEquals("s", SimpleModel__.simpleValue.data("simple.string"));
        assertEquals(expectedType, SimpleModel__.simpleValue.data("simple.type"));
        assertEquals("literal", SimpleModel__.simpleValue.data("simple.literal"));
    }

    @Test public void assert_that_qualifier_extension_works() {
        final Qualifier<SimpleModel> q = SimpleModelMetadata;
        assertEquals("string value", q.data(Extension.<String>key("extension.string")));
        assertEquals(true, q.data(Extension.<Boolean>key("extension.boolean")));
        assertEquals(Integer.valueOf(1), q.data(Extension.<Integer>key("extension.int")));
        assertEquals(TimeUnit.SECONDS, q.data(Extension.<TimeUnit>key("extension.enum")));
        assertEquals(Color.valueOf("red"), q.data(Extension.<Color>key("extension.valueOf")));
        assertEquals(String.class, q.data(Extension.<Class<?>>key("extension.class")));
    }

    @Test public void test_works() throws Exception {
        assertNotNull(SimpleModelMetadata);
        assertNotNull(SimpleInnerMetadata);
        assertEquals(List.class, stringListValue.getType());
    }
}
