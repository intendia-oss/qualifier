package com.intendia.qualifier.processor;

import static com.intendia.qualifier.processor.SimpleModelSimpleInner__.SimpleInnerMetadata;
import static com.intendia.qualifier.processor.SimpleModel__.SimpleModelMetadata;
import static com.intendia.qualifier.processor.SimpleModel__.stringListValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.intendia.qualifier.processor.SimpleModel.SimpleInnerInterface;
import java.util.List;
import org.junit.Test;

public class SimpleProcessorExtensionTest {
    @Test
    public void test_processor_extensions_are_executed() {
        final Object expectedType = SimpleInnerInterface.class;
        assertNotNull(SimpleModelMetadata.simpleValue().getContext().get("simple.loaded"));
        assertEquals(1, SimpleModelMetadata.simpleValue().getContext().get("simple.getInteger"));
        assertEquals("s", SimpleModelMetadata.simpleValue().getContext().get("simple.getString"));
        assertEquals(expectedType, SimpleModelMetadata.simpleValue().getContext().get("simple.getType"));
        assertEquals(Object.class, SimpleModelMetadata.simpleValue().getContext().get("simple.getLiteral").getClass());
    }

    @Test
    public void test_works() throws Exception {
        assertNotNull(SimpleModelMetadata);
        assertNotNull(SimpleInnerMetadata);
        assertEquals(List.class, stringListValue.getType());
    }
}
