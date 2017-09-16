package com.intendia.qualifier.processor;

import java.util.Arrays;
import javax.annotation.processing.Processor;

public class ProcessorTestUtils {
    public static Iterable<? extends Processor> qualifierProcessors() {
        return Arrays.asList(new StaticQualifierMetamodelProcessor());
    }
}
