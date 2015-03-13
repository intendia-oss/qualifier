package com.intendia.qualifier.processor;

import com.intendia.qualifier.annotation.Qualify;
import com.intendia.qualifier.annotation.QualifyExtension;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Qualify(extend = {
        @QualifyExtension(key = "extension.string", type = String.class, value = "string value"),
        @QualifyExtension(key = "extension.int", type = Integer.class, value = "1"),
        @QualifyExtension(key = "extension.valueOf", type = TimeUnit.class, value = "SECONDS"),
        @QualifyExtension(key = "extension.class", type = Class.class, value = "java.lang.String"),
})
public interface SimpleModel {
    @Simple(getString = "s", getType = SimpleInnerInterface.class, getInteger = 1)
    public String getSimpleValue();

    public List<String> getStringListValue();

    @Qualify
    static class SimpleInner {
    }

    @Qualify
    @Simple(getString = "s", getType = SimpleInnerInterface.class, getInteger = 1)
    interface SimpleDependant extends com.google.common.base.Predicate<SimpleInnerInterface> {
    }

    @Qualify
    interface SimpleInnerInterface {
        public List<String> getVehicleParam();

        public void setVehicleParam(List<String> vehicleParam);
    }
}
