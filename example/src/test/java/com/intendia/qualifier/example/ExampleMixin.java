package com.intendia.qualifier.example;

import com.intendia.qualifier.annotation.Qualify;
import com.intendia.qualifier.annotation.QualifyExtension;

@Qualify
public interface ExampleMixin {

    @Qualify(extend = @QualifyExtension(key = "mixin.val", type = String.class, value = "mixin"))
    String getStringValue();
}
