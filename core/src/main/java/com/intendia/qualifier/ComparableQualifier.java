// Copyright 2015 Intendia, SL.
package com.intendia.qualifier;

import com.google.common.collect.Ordering;

public interface ComparableQualifier<T> extends Qualifier<T> {
    // TODO choose comparator in processor
    default Ordering<T> getOrdering() { return Ordering.usingToString().nullsFirst(); }

    static <T> ComparableQualifier<T> of(Qualifier<T> q) {
        return q instanceof ComparableQualifier ? (ComparableQualifier<T>) q : q::data;
    }
}
