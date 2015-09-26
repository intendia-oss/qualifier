// Copyright 2015 Intendia, SL.
package com.intendia.qualifier.example;

import static java.util.concurrent.TimeUnit.SECONDS;

import com.google.common.base.Predicate;
import com.intendia.qualifier.annotation.Qualify;
import com.intendia.qualifier.annotation.QualifyExtension;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Qualify(extend = {
        @QualifyExtension(key = "extension.string", type = String.class, value = "string value"),
        @QualifyExtension(key = "extension.boolean", type = Boolean.class, value = "true"),
        @QualifyExtension(key = "extension.int", type = Integer.class, value = "1"),
        @QualifyExtension(key = "extension.enum", type = TimeUnit.class, value = "SECONDS"),
        @QualifyExtension(key = "extension.valueOf", type = ExampleModel.Color.class, value = "red"),
        @QualifyExtension(key = "extension.class", type = Class.class, value = "java.lang.String"),
})
public interface ExampleModel {
    @ExampleAuto(string = "s", type = ExampleInnerInterface.class, integer = 1, enumeration = SECONDS)
    @ExampleManual(string = "s", type = ExampleInnerInterface.class, integer = 1, enumeration = SECONDS) //
    String getStringValue();

    List<String> getStringListValue();

    @Qualify class ExampleInner {}

    @ExampleAuto(string = "s", type = ExampleInnerInterface.class, integer = 1, enumeration = SECONDS)
    @ExampleManual(string = "s", type = ExampleInnerInterface.class, integer = 1, enumeration = SECONDS)
    @Qualify interface ExampleDependant extends Predicate<ExampleInnerInterface> {}

    @Qualify interface ExampleInnerInterface {
        List<String> getVehicleParam();

        void setVehicleParam(List<String> vehicleParam);
    }

    class Color {
        private final String color;

        public Color(String color) { this.color = color; }

        public String getColor() { return color; }

        @Override public boolean equals(Object o) { return this == o || o instanceof Color && equals((Color) o); }

        public boolean equals(Color o) { return Objects.equals(color, o.color); }

        @Override public int hashCode() { return Objects.hash(color); }

        public static Color valueOf(String color) { return new Color(color); }
    }
}
