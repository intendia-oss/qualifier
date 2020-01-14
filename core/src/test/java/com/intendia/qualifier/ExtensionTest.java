package com.intendia.qualifier;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ExtensionTest {

    @Test public void named_extensions_returns_its_own_key() {
        assertEquals("my.extension", Extension.key("my.extension").getKey());
    }

    @Test(expected = UnsupportedOperationException.class) public void anonymous_throws_if_try_to_get_the_key() {
        Extension.anonymous().getKey();
    }
}
