package com.intendia.qualifier.processor;

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
        @QualifyExtension(key = "extension.valueOf", type = SimpleModel.Color.class, value = "red"),
        @QualifyExtension(key = "extension.class", type = Class.class, value = "java.lang.String"),
})
public interface SimpleModel {
    @Simple(getString = "s", getType = SimpleInnerInterface.class, getInteger = 1) String getSimpleValue();

    List<String> getStringListValue();

    @Qualify class SimpleInner {}

    @Qualify @Simple(getString = "s", getType = SimpleInnerInterface.class, getInteger = 1)
    interface SimpleDependant extends Predicate<SimpleInnerInterface> {}

    @Qualify interface SimpleInnerInterface {
        List<String> getVehicleParam();

        void setVehicleParam(List<String> vehicleParam);
    }

    class Color {
        private final String color;

        public Color(String color) { this.color = color; }

        public String getColor() { return color; }

        public static Color valueOf(String color) { return new Color(color); }
    }
}
