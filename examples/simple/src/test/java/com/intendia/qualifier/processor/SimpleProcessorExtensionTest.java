package com.intendia.qualifier.processor;

import static com.intendia.qualifier.processor.SimpleModelSimpleInner__.SimpleInnerMetadata;
import static com.intendia.qualifier.processor.SimpleModel__.SimpleModelMetadata;
import static com.intendia.qualifier.processor.SimpleModel__.stringListValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.List;
import org.junit.Test;

public class SimpleProcessorExtensionTest {
    @Test
    public void test_processor_extensions_are_executed() {
        assertNotNull(SimpleModelMetadata.simpleValue().getContext().get("simple.loaded"));
    }

    @Test
    public void test_works() throws Exception {
        assertNotNull(SimpleModelMetadata);
        assertNotNull(SimpleInnerMetadata);
        assertEquals(List.class, stringListValue.getType());
    }
}
