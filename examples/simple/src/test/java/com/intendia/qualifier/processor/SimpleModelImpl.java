// Copyright 2015 Intendia, SL.
package com.intendia.qualifier.processor;

import com.intendia.qualifier.annotation.Qualify;
import java.util.List;

@Qualify
public class SimpleModelImpl implements SimpleModel {
    String simpleValue;
    List<String> stringListValue;
    Double implDouble;

    @Override public String getSimpleValue() { return simpleValue; }

    @Override public List<String> getStringListValue() { return stringListValue; }

    public Double getImplDouble() { return implDouble; }
}
