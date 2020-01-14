package com.intendia.qualifier.example;

import com.intendia.qualifier.Qualifier;

public interface ManualMixin {
    Qualifier<int[]> intArray = Qualifier.create(int[].class);
}
