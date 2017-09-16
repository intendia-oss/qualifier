package com.intendia.qualifier.example;

import com.intendia.qualifier.annotation.Qualify;
import com.intendia.qualifier.annotation.Qualify.Extend;
import java.util.List;

@Qualify @Extend(ExampleModel.class)
public class ExampleModelImpl implements ExampleModel {
    String stringValue;
    List<String> stringListValue;
    Color color;
    private int[] intArray;
    private Integer[] integerArray;
    Double doubleValue;

    @Override public String getStringValue() {
        return stringValue;
    }

    @Override public void setStringValue(String stringValue) {
        this.stringValue = stringValue;
    }

    @Override public List<String> getStringListValue() {
        return stringListValue;
    }

    @Override public Color getColorValue() {
        return color;
    }

    @Override public int[] getIntArray() {
        return intArray;
    }

    @Override public Integer[] getIntegerArray() {
        return integerArray;
    }

    public Double getDoubleValue() {
        return doubleValue;
    }
}
