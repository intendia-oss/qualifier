// Copyright 2015 Intendia, SL.
package com.intendia.qualifier.example;

import static java.util.concurrent.TimeUnit.DAYS;
import static java.util.concurrent.TimeUnit.HOURS;
import static java.util.concurrent.TimeUnit.SECONDS;

import com.google.common.base.Predicate;
import com.intendia.qualifier.annotation.Qualify;
import com.intendia.qualifier.annotation.Qualify.Entry;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Qualify(mixin = ExampleMixin.class, extend = {
        @Entry(key = "extension.string", type = String.class, value = "string value"),
        @Entry(key = "extension.boolean", type = Boolean.class, value = "true"),
        @Entry(key = "extension.int", type = Integer.class, value = "1"),
        @Entry(key = "extension.enum", type = TimeUnit.class, value = "SECONDS"),
        @Entry(key = "extension.valueOf", type = Color.class, value = "red"),
        @Entry(key = "extension.class", type = Class.class, value = "java.lang.String"),
})
public interface ExampleModel {
    @ExampleAuto(string = "s", type = ExampleInnerInterface.class, link = Color.class, integer = 1, enumeration = SECONDS, enumerationList = {DAYS, HOURS})
    @ExampleManual(string = "s", type = ExampleInnerInterface.class, integer = 1, enumeration = SECONDS) //
    @Qualify.Extend(ExampleMixin.class) //
    String getStringValue();

    void setStringValue(String stringValue);

    List<String> getStringListValue();

    Color getColorValue();

    int[] getIntArray();

    Integer[] getIntegerArray();

    default List<Integer> getIntegerList() {
        return Arrays.asList(getIntegerArray());
    }

    @Qualify class ExampleInner {}

    @ExampleAuto(string = "s", type = ExampleInnerInterface.class, link = Color.class, integer = 1, enumeration = SECONDS)
    @ExampleManual(string = "s", type = ExampleInnerInterface.class, integer = 1, enumeration = SECONDS)
    @Qualify interface ExampleDependant extends Predicate<ExampleInnerInterface> {}

    class Category {
        @Qualify.Extend(value = ExampleMixin.class, name = "stringValue") //
        public static String getCategoryString(ExampleModel o) {
            return o.getStringValue();
        }

        public static void setCategoryString(ExampleModel o, String v) {
            o.setStringValue(v);
        }
    }

    @Qualify interface ExampleInnerInterface {
        List<String> getInnerListValue();

        void setInnerListValue(List<String> innerListValue);
    }
}
