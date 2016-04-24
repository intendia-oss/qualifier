// Copyright 2015 Intendia, SL.
package com.intendia.qualifier.example;

import com.intendia.qualifier.annotation.Qualify;
import java.util.Arrays;
import java.util.List;

@Qualify(fields = true)
public class ExampleFields {
    public String stringValue;
    public List<String> stringListValue;
    public Color colorValue;
    public int[] intArray;
    public Integer[] integerArray;

    public List<Integer> getIntegerList() {
        return Arrays.asList(integerArray);
    }

    public static class Category {
        public static String getCategoryString(ExampleFields o) {
            return o.stringValue;
        }
    }
}
