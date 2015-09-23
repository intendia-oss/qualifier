// Copyright 2013 Intendia, SL.
package com.intendia.qualifier;

import static com.google.common.base.MoreObjects.firstNonNull;

public final class Qualifiers {

    private Qualifiers() {}

    public static String getString(Qualifier<?> qualifier, String extensionName) {
        return (String) qualifier.data(extensionName);
    }

    public static String getString(Qualifier<?> qualifier, String extensionName, String defaultValue) {
        return firstNonNull(getString(qualifier, extensionName), defaultValue);
    }
}
