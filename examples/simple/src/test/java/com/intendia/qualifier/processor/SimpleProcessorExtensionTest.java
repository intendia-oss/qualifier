package com.intendia.qualifier.processor;

import static org.junit.Assert.assertNotNull;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

public class SimpleProcessorExtensionTest {
    @Test
    @Ignore("not working")
    public void test_processor_extensions_are_executed() {
        Assert.assertNotNull(SimpleModel__.self.getContext().get("simple.loaded"));
    }

    @Test
    public void test_works() throws Exception {
        assertNotNull(SimpleModel__.self);
        assertNotNull(SimpleModel$SimpleInner__.self);
    }
}
