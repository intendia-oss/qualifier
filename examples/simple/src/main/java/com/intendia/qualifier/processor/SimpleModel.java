package com.intendia.qualifier.processor;

import com.intendia.qualifier.annotation.Qualify;
import com.intendia.qualifier.annotation.QualifyExtension;
import java.util.concurrent.TimeUnit;

@Qualify(extend = {
        @QualifyExtension(key = "extension.string", type = String.class, value = "string value"),
        @QualifyExtension(key = "extension.int", type = Integer.class, value = "1"),
        @QualifyExtension(key = "extension.valueOf", type = TimeUnit.class, value = "SECONDS"),
        @QualifyExtension(key = "extension.class", type = Class.class, value = "java.lang.String"),
})
public interface SimpleModel {
    @Simple(getString = "s", getType = String.class, getInteger = 1)
    public String getSimpleValue();
}
