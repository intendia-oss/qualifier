package com.intendia.qualifier;

import java.util.Map;

public interface StaticQualifierLoader {
    /** Return all static bean qualifier instances, i.e. the self property in each static qualifiers. */
    public Map<Class<? extends Qualifier<?>>, Qualifier<?>> getBeanQualifiers();
}
