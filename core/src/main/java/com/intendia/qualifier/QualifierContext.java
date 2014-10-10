// Copyright 2014 Intendia, SL.
package com.intendia.qualifier;

import javax.annotation.Nullable;

public interface QualifierContext {

    @Nullable
    <T> T getQualifier(String key);

    <T extends ResourceProvider<?>> T getResourceProvider(Class<T> resourceType, String extensionKey);

}
