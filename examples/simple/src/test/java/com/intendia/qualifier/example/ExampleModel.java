// Copyright 2015 Intendia, SL.
package com.intendia.qualifier.example;

import static java.util.concurrent.TimeUnit.SECONDS;

import com.google.common.base.Predicate;
import com.intendia.qualifier.annotation.Qualify;
import com.intendia.qualifier.annotation.QualifyExtension;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Qualify(extend = {
        @QualifyExtension(key = "extension.string", type = String.class, value = "string value"),
        @QualifyExtension(key = "extension.boolean", type = Boolean.class, value = "true"),
        @QualifyExtension(key = "extension.int", type = Integer.class, value = "1"),
        @QualifyExtension(key = "extension.enum", type = TimeUnit.class, value = "SECONDS"),
        @QualifyExtension(key = "extension.valueOf", type = Color.class, value = "red"),
        @QualifyExtension(key = "extension.class", type = Class.class, value = "java.lang.String"),
})
public interface ExampleModel {
    @ExampleAuto(string = "s", type = ExampleInnerInterface.class, link = Color.class, integer = 1, enumeration = SECONDS)
    @ExampleManual(string = "s", type = ExampleInnerInterface.class, integer = 1, enumeration = SECONDS) //
    String getStringValue();

    void setStringValue(String stringValue);

    List<String> getStringListValue();

    Color getColorValue();

    @Qualify class ExampleInner {}

    @ExampleAuto(string = "s", type = ExampleInnerInterface.class, link = Color.class, integer = 1, enumeration = SECONDS)
    @ExampleManual(string = "s", type = ExampleInnerInterface.class, integer = 1, enumeration = SECONDS)
    @Qualify interface ExampleDependant extends Predicate<ExampleInnerInterface> {}

    class Category {
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
