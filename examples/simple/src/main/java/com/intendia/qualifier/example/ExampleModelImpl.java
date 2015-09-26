// Copyright 2015 Intendia, SL.
package com.intendia.qualifier.example;

import com.intendia.qualifier.annotation.Qualify;
import java.util.List;

@Qualify
public class ExampleModelImpl implements ExampleModel {
    String stringValue;
    List<String> stringListValue;
    Double doubleValue;

    @Override public String getStringValue() { return stringValue; }

    @Override public List<String> getStringListValue() { return stringListValue; }

    public Double getDoubleValue() { return doubleValue; }
}
