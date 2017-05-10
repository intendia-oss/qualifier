package com.intendia.qualifier.example;

import com.intendia.qualifier.annotation.Qualify;
import com.intendia.qualifier.annotation.Qualify.Entry;

@Qualify
public interface ExampleMixin {

    @Qualify(extend = @Entry(key = "mixin.val", type = String.class, value = "mixin"))
    Object getStringValue();

    Object getStringListValue();

    Object getColorValue();

    Object getIntArray();

    Object getIntegerArray();

    Object getIntegerList();
}
